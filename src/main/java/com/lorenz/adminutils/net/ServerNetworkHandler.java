package com.lorenz.adminutils.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.api.AdminPermission;
import com.lorenz.adminutils.audit.AuditLog;
import com.lorenz.adminutils.disguise.DisguiseRegistry;
import com.lorenz.adminutils.net.payload.ActionRequestC2S;
import com.lorenz.adminutils.net.payload.ActionResponseS2C;
import com.lorenz.adminutils.net.payload.HelloC2S;
import com.lorenz.adminutils.net.payload.HelloS2C;
import com.lorenz.adminutils.net.payload.PermissionQueryC2S;
import com.lorenz.adminutils.net.payload.PermissionSnapshotS2C;
import com.lorenz.adminutils.net.payload.PlayerInventoryChunkS2C;
import com.lorenz.adminutils.net.payload.PlayerInventoryRequestC2S;
import com.lorenz.adminutils.permissions.PermissionService;
import com.lorenz.adminutils.players.ContainerAccessPolicy;
import com.lorenz.adminutils.players.PlayerContainerKind;
import com.lorenz.adminutils.players.PlayerInventorySnapshotFactory;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side packet entry points.
 *
 * <p>Every handler resolves the acting player from {@code context.player()}, which is
 * the authenticated connection, and re-checks permissions against the live server
 * state. Nothing supplied by the client is trusted beyond the action identifier.
 */
public final class ServerNetworkHandler {
	private static final RateLimiter LIMITER = new RateLimiter(20, 5.0);
	/**
	 * Separate bucket for container reads so inventory polling cannot starve the action
	 * channel, and so a modified client cannot turn reads into a flood.
	 */
	private static final RateLimiter READ_LIMITER = new RateLimiter(10, 2.0);
	private static final AtomicInteger SNAPSHOT_SEQUENCE = new AtomicInteger();

	private static PermissionService permissions;
	private static AuditLog audit;

	private ServerNetworkHandler() {
	}

	public static void register(PermissionService service, AuditLog auditLog) {
		permissions = service;
		audit = auditLog;

		ServerPlayNetworking.registerGlobalReceiver(HelloC2S.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleHello(player, payload));
		});

		ServerPlayNetworking.registerGlobalReceiver(PermissionQueryC2S.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleQuery(player));
		});

		ServerPlayNetworking.registerGlobalReceiver(ActionRequestC2S.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleAction(player, payload));
		});

		ServerPlayNetworking.registerGlobalReceiver(PlayerInventoryRequestC2S.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			MinecraftServer server = context.server();
			server.execute(() -> handleInventoryRequest(server, player, payload));
		});

		AdminUtils.LOGGER.info("Registered server receivers for {} inbound payload types", 4);
	}

	public static void forget(UUID player) {
		LIMITER.forget(player);
		READ_LIMITER.forget(player);
	}

	public static void sendSnapshot(ServerPlayer player) {
		if (permissions == null || player == null) {
			return;
		}
		ServerPlayNetworking.send(player, PermissionSnapshotS2C.of(permissions.snapshot(player)));
	}

	private static void handleHello(ServerPlayer player, HelloC2S payload) {
		Optional<ErrorCode> invalid = RequestValidator.validate(payload);
		if (invalid.isPresent()) {
			ServerPlayNetworking.send(player, new HelloS2C(Protocol.VERSION, false, AdminUtils.modVersion(), "Invalid handshake"));
			audit.record("handshake.invalid", player, "malformed hello payload", false);
			return;
		}

		boolean compatible = payload.protocolVersion() == Protocol.VERSION;
		String message = compatible
			? "ok"
			: "Protocol mismatch: client " + payload.protocolVersion() + ", server " + Protocol.VERSION;

		ServerPlayNetworking.send(player, new HelloS2C(Protocol.VERSION, compatible, AdminUtils.modVersion(), message));
		audit.record("handshake", player, "client=" + payload.clientVersion() + " protocol=" + payload.protocolVersion(), compatible);

		if (compatible) {
			sendSnapshot(player);
		}
	}

	private static void handleQuery(ServerPlayer player) {
		if (!LIMITER.tryAcquire(player.getUUID())) {
			audit.record("permissions.query", player, "rate limited", false);
			return;
		}
		sendSnapshot(player);
	}

	private static void handleAction(ServerPlayer player, ActionRequestC2S payload) {
		String requestId = payload == null || payload.requestId() == null ? "" : payload.requestId();

		if (!LIMITER.tryAcquire(player.getUUID())) {
			ServerPlayNetworking.send(player, ActionResponseS2C.error(requestId, ErrorCode.RATE_LIMITED, "Too many requests"));
			audit.record("action.rate_limited", player, requestId, false);
			return;
		}

		Optional<ErrorCode> invalid = RequestValidator.validate(payload);
		if (invalid.isPresent()) {
			ServerPlayNetworking.send(player, ActionResponseS2C.error(requestId, invalid.get(), "Malformed request"));
			audit.record("action.invalid", player, requestId, false);
			return;
		}

		Optional<ServerAction> resolved = ServerAction.byId(payload.actionId());
		if (resolved.isEmpty()) {
			ServerPlayNetworking.send(player, ActionResponseS2C.error(requestId, ErrorCode.UNSUPPORTED_ACTION, "Unknown action"));
			audit.record("action.unsupported", player, payload.actionId(), false);
			return;
		}

		ServerAction action = resolved.get();
		if (!permissions.has(player, action.permission())) {
			ServerPlayNetworking.send(player, ActionResponseS2C.error(requestId, ErrorCode.PERMISSION_DENIED, "Permission denied"));
			audit.record("action.denied", player, action.id(), false);
			return;
		}

		switch (action) {
			case PERMISSIONS_REFRESH -> {
				sendSnapshot(player);
				ServerPlayNetworking.send(player, ActionResponseS2C.ok(requestId, "Permissions refreshed"));
			}
			case DISGUISE_STATUS -> ServerPlayNetworking.send(player,
				ActionResponseS2C.ok(requestId, "disguise available=" + DisguiseRegistry.isAvailable()));
		}

		audit.record("action." + action.id(), player, payload.argument(), true);
	}

	/**
	 * Read-only inventory snapshot.
	 *
	 * <p>The actor is the connection, never the payload. The requested UUID only selects
	 * a target from the server player list, so an offline or unknown UUID cannot reach
	 * stored player data. Permission is re-checked here on every request regardless of
	 * what the client believes its snapshot says.
	 */
	private static void handleInventoryRequest(MinecraftServer server, ServerPlayer actor,
		PlayerInventoryRequestC2S payload) {
		String requestId = payload == null || payload.requestId() == null ? "" : payload.requestId();

		if (!READ_LIMITER.tryAcquire(actor.getUUID())) {
			sendError(actor, requestId, ErrorCode.RATE_LIMITED, "Too many requests");
			audit.record("players.inventory.rate_limited", actor, "container=inventory", false);
			return;
		}

		Optional<ErrorCode> invalid = RequestValidator.validate(payload);
		if (invalid.isPresent()) {
			sendError(actor, requestId, invalid.get(), "Malformed request");
			audit.record("players.inventory.invalid", actor, "container=inventory", false);
			return;
		}

		boolean permitted = permissions.has(actor, AdminPermission.PLAYERS_INVENTORY_VIEW);
		ServerPlayer target = server.getPlayerList().getPlayer(payload.target());
		ContainerAccessPolicy.Decision decision = ContainerAccessPolicy.decideRead(permitted, target != null);

		if (decision == ContainerAccessPolicy.Decision.DENY_PERMISSION) {
			sendError(actor, requestId, ErrorCode.PERMISSION_DENIED, "Permission denied");
			audit.record("players.inventory.denied", actor, "target=" + payload.target() + " container=inventory", false);
			return;
		}
		if (decision == ContainerAccessPolicy.Decision.TARGET_UNAVAILABLE) {
			sendError(actor, requestId, ErrorCode.TARGET_UNAVAILABLE, "Target is not online");
			audit.record("players.inventory.unavailable", actor, "target=" + payload.target() + " container=inventory", false);
			return;
		}

		Inventory inventory = target.getInventory();
		if (!ContainerAccessPolicy.hasExpectedSize(inventory.getContainerSize(), PlayerContainerKind.INVENTORY)) {
			sendError(actor, requestId, ErrorCode.INVALID_SLOT, "Unsupported inventory layout");
			audit.record("players.inventory.unsupported", actor, "target=" + target.getUUID() + " container=inventory", false);
			return;
		}

		int size = inventory.getContainerSize();
		List<ItemStack> slots = new ArrayList<>(size);
		for (int slot = 0; slot < size; slot++) {
			// copy() detaches the snapshot from the live inventory: the target can keep
			// playing while the response is encoded.
			slots.add(inventory.getItem(slot).copy());
		}

		PlayerInventorySnapshotFactory.Result snapshot = PlayerInventorySnapshotFactory.build(
			requestId, target.getUUID(), SNAPSHOT_SEQUENCE.incrementAndGet(), slots, server.registryAccess());

		if (snapshot.rejected()) {
			sendError(actor, requestId, ErrorCode.SNAPSHOT_TOO_LARGE, "Inventory snapshot exceeds the transfer limit");
			audit.record("players.inventory.too_large", actor,
				"target=" + target.getUUID() + " container=inventory reason=" + snapshot.rejection(), false);
			return;
		}

		if (!ServerPlayNetworking.canSend(actor, PlayerInventoryChunkS2C.TYPE)) {
			// The client cannot decode a chunk, so nothing is sent on that channel. The
			// failure is logged and audited, and the requester is told on the action
			// channel only when that channel is available too.
			AdminUtils.LOGGER.warn(
				"Cannot send inventory snapshot to {}: client does not accept {} (target={}, chunks={})",
				actor.getUUID(), PlayerInventoryChunkS2C.TYPE.id(), target.getUUID(), snapshot.chunks().size());
			audit.record("players.inventory.unsupported_client", actor,
				"target=" + target.getUUID() + " container=inventory chunks=" + snapshot.chunks().size(), false);
			sendError(actor, requestId, ErrorCode.UNSUPPORTED_ACTION, "Client cannot receive inventory snapshots");
			return;
		}

		for (PlayerInventoryChunkS2C chunk : snapshot.chunks()) {
			ServerPlayNetworking.send(actor, chunk);
		}

		audit.record("players.inventory.view", actor,
			"target=" + target.getUUID() + " container=inventory chunks=" + snapshot.chunks().size(), true);
	}

	/**
	 * Sends an error on the action channel, but only when the client registered a receiver
	 * for it. Sending to a channel the client cannot decode would disconnect the player.
	 */
	private static void sendError(ServerPlayer player, String requestId, ErrorCode code, String message) {
		if (!ServerPlayNetworking.canSend(player, ActionResponseS2C.TYPE)) {
			AdminUtils.LOGGER.warn("Cannot deliver {} to {}: client does not accept {}",
				code.id(), player.getUUID(), ActionResponseS2C.TYPE.id());
			return;
		}
		ServerPlayNetworking.send(player, ActionResponseS2C.error(requestId, code, message));
	}
}
