package com.lorenz.adminutils.net.payload;

import com.lorenz.adminutils.net.Protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Handshake result. {@code accepted == false} means the client must not send any
 * further AdminUtils packets for this session.
 */
public record HelloS2C(int protocolVersion, boolean accepted, String serverVersion, String message) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HelloS2C> TYPE = new CustomPacketPayload.Type<>(Protocol.channel("hello_ack"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HelloS2C> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, HelloS2C::protocolVersion,
		ByteBufCodecs.BOOL, HelloS2C::accepted,
		ByteBufCodecs.STRING_UTF8, HelloS2C::serverVersion,
		ByteBufCodecs.STRING_UTF8, HelloS2C::message,
		HelloS2C::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
