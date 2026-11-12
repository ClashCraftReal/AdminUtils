package com.lorenz.adminutils.net;

import java.util.Optional;
import java.util.UUID;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.audit.AuditLog;
import com.lorenz.adminutils.disguise.DisguiseRegistry;
import com.lorenz.adminutils.net.payload.ActionRequestC2S;
import com.lorenz.adminutils.net.payload.ActionResponseS2C;
import com.lorenz.adminutils.net.payload.HelloC2S;
import com.lorenz.adminutils.net.payload.HelloS2C;
import com.lorenz.adminutils.net.payload.PermissionQueryC2S;
import com.lorenz.adminutils.net.payload.PermissionSnapshotS2C;
import com.lorenz.adminutils.permissions.PermissionService;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side packet entry points.
 *
 * <p>Every handler resolves the acting player from {@code context.player()}, which is
 * the authenticated connection, and re-checks permissions against the live server
 * state. Nothing supplied by the client is trusted beyond the action identifier.
 */
public final class ServerNetworkHandler {
	private static final RateLimiter LIMITER = new RateLimiter(20, 5.0);

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

		AdminUtils.LOGGER.info("Registered server receivers for {} inbound payload types", 3);
	}

	public static void forget(UUID player) {
		LIMITER.forget(player);
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
}
