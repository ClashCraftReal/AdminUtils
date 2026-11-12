package com.lorenz.adminutils.net.payload;

import com.lorenz.adminutils.net.Protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Asks the server to re-send the caller's permission snapshot. The server answers
 * for the authenticated connection only; the packet carries no player reference.
 */
public record PermissionQueryC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PermissionQueryC2S> TYPE =
		new CustomPacketPayload.Type<>(Protocol.channel("permission_query"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PermissionQueryC2S> STREAM_CODEC =
		StreamCodec.unit(new PermissionQueryC2S());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
