package com.lorenz.adminutils.net.payload;

import com.lorenz.adminutils.net.Protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A privileged action request. {@code actionId} is a server-side allow-listed key;
 * the client cannot name a permission, a player UUID or an authorization level.
 */
public record ActionRequestC2S(String requestId, String actionId, String argument) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ActionRequestC2S> TYPE =
		new CustomPacketPayload.Type<>(Protocol.channel("action_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ActionRequestC2S> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, ActionRequestC2S::requestId,
		ByteBufCodecs.STRING_UTF8, ActionRequestC2S::actionId,
		ByteBufCodecs.STRING_UTF8, ActionRequestC2S::argument,
		ActionRequestC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
