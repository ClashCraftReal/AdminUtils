package com.lorenz.adminutils.client.net;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.net.ErrorCode;
import com.lorenz.adminutils.net.Protocol;
import com.lorenz.adminutils.net.payload.ActionResponseS2C;
import com.lorenz.adminutils.net.payload.HelloC2S;
import com.lorenz.adminutils.net.payload.HelloS2C;
import com.lorenz.adminutils.net.payload.PermissionQueryC2S;
import com.lorenz.adminutils.net.payload.PermissionSnapshotS2C;
import com.lorenz.adminutils.net.payload.PlayerInventoryChunkS2C;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side receivers and connection lifecycle. The client only ever reacts to
 * server state; it never asserts permissions of its own.
 */
public final class ClientNetworkHandler {
	private ClientNetworkHandler() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(HelloS2C.TYPE, (payload, context) ->
			context.client().execute(() -> handleHello(payload)));

		ClientPlayNetworking.registerGlobalReceiver(PermissionSnapshotS2C.TYPE, (payload, context) ->
			context.client().execute(() -> {
				ClientSession.updateSnapshot(payload.toSnapshot());
				AdminUtils.LOGGER.info("Permission snapshot received: enabled={} operator={} opBypass={} granted={}",
					payload.enabled(), payload.operator(), payload.opBypass(), payload.permissions());
			}));

		ClientPlayNetworking.registerGlobalReceiver(ActionResponseS2C.TYPE, (payload, context) ->
			context.client().execute(() -> {
				ClientSession.dispatchResponse(payload);
				// A failed container request answers on the action channel, so the
				// inventory view learns about denials and unavailable targets here.
				PlayerInventoryStore.shared().onError(payload.requestId(), errorCodeOf(payload));
			}));

		ClientPlayNetworking.registerGlobalReceiver(PlayerInventoryChunkS2C.TYPE, (payload, context) ->
			context.client().execute(() -> PlayerInventoryStore.shared().onChunk(payload)));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			// A new connection must not inherit a request or an error from the previous
			// session; otherwise the first inventory view of a login can stay stuck.
			PlayerInventoryStore.shared().clear();
			ClientSession.update(ConnectionState.HANDSHAKING, "Negotiating protocol");
			ClientPlayNetworking.send(new HelloC2S(Protocol.VERSION, AdminUtils.modVersion()));
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientSession.reset();
			PlayerInventoryStore.shared().clear();
		});

		AdminUtils.LOGGER.info("Registered client receivers for 4 inbound payload types");
	}

	public static void requestPermissions() {
		if (ClientSession.state() != ConnectionState.READY) {
			return;
		}
		ClientPlayNetworking.send(new PermissionQueryC2S());
	}

	private static void handleHello(HelloS2C payload) {
		if (payload.accepted()) {
			ClientSession.update(ConnectionState.READY, "");
			ClientPlayNetworking.send(new PermissionQueryC2S());
			return;
		}

		boolean mismatch = payload.protocolVersion() != Protocol.VERSION;
		ClientSession.update(mismatch ? ConnectionState.INCOMPATIBLE : ConnectionState.ERROR, payload.message());
		AdminUtils.LOGGER.warn("AdminUtils handshake rejected: {}", payload.message());
	}

	public static ErrorCode errorCodeOf(ActionResponseS2C response) {
		return ErrorCode.byId(response.errorCode()).orElse(ErrorCode.INTERNAL_ERROR);
	}
}
