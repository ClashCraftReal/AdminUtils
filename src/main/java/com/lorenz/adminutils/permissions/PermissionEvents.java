package com.lorenz.adminutils.permissions;

import com.lorenz.adminutils.audit.AuditLog;
import com.lorenz.adminutils.net.ServerNetworkHandler;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Wires permission state to connection lifecycle so a reload or a config edit is
 * reflected immediately for players who are already online.
 */
public final class PermissionEvents {
	private PermissionEvents() {
	}

	public static void register(ServerPermissionService service, AuditLog audit) {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			ServerNetworkHandler.sendSnapshot(player);
			audit.record("session.join", player, "permission snapshot pushed", true);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player != null) {
				ServerNetworkHandler.forget(handler.player.getUUID());
			}
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			service.reload();
			pushAll(server, service);
		});
	}

	/**
	 * Re-sends the snapshot to every connected player. Call after a reload so revoked
	 * permissions disappear from open menus without a reconnect.
	 */
	public static void pushAll(MinecraftServer server, PermissionService service) {
		if (server == null || service == null) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerNetworkHandler.sendSnapshot(player);
		}
	}
}
