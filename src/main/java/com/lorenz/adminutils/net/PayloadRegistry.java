package com.lorenz.adminutils.net;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.net.payload.ActionRequestC2S;
import com.lorenz.adminutils.net.payload.ActionResponseS2C;
import com.lorenz.adminutils.net.payload.HelloC2S;
import com.lorenz.adminutils.net.payload.HelloS2C;
import com.lorenz.adminutils.net.payload.PermissionQueryC2S;
import com.lorenz.adminutils.net.payload.PermissionSnapshotS2C;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers every AdminUtils payload type exactly once.
 *
 * <p>Version-specific workaround: Fabric API renamed the play-channel registry
 * accessors in the 26.x line. 1.21.11 exposes {@code playC2S()/playS2C()}, while
 * 26.1+ exposes {@code serverboundPlay()/clientboundPlay()}. The payload records
 * themselves are identical on both, so only this registration block is duplicated.
 */
public final class PayloadRegistry {
	private static final int PAYLOAD_COUNT = 6;
	private static boolean registered;

	private PayloadRegistry() {
	}

	public static void register() {
		if (registered) {
			return;
		}
		registered = true;

		//? if <26.1 {
		/*PayloadTypeRegistry.playC2S().register(HelloC2S.TYPE, HelloC2S.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(PermissionQueryC2S.TYPE, PermissionQueryC2S.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ActionRequestC2S.TYPE, ActionRequestC2S.STREAM_CODEC);

		PayloadTypeRegistry.playS2C().register(HelloS2C.TYPE, HelloS2C.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PermissionSnapshotS2C.TYPE, PermissionSnapshotS2C.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ActionResponseS2C.TYPE, ActionResponseS2C.STREAM_CODEC);
		*///?} else {
		PayloadTypeRegistry.serverboundPlay().register(HelloC2S.TYPE, HelloC2S.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PermissionQueryC2S.TYPE, PermissionQueryC2S.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ActionRequestC2S.TYPE, ActionRequestC2S.STREAM_CODEC);

		PayloadTypeRegistry.clientboundPlay().register(HelloS2C.TYPE, HelloS2C.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PermissionSnapshotS2C.TYPE, PermissionSnapshotS2C.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ActionResponseS2C.TYPE, ActionResponseS2C.STREAM_CODEC);
		//?}

		AdminUtils.LOGGER.info("Registered {} payload types for protocol v{}", PAYLOAD_COUNT, Protocol.VERSION);
	}

	public static boolean isRegistered() {
		return registered;
	}
}
