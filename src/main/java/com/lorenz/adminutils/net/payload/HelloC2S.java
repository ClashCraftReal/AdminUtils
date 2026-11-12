package com.lorenz.adminutils.net.payload;

import com.lorenz.adminutils.net.Protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * First client-to-server packet after joining. Carries only the client's protocol
 * version and mod version; no identity or permission data is accepted from it.
 */
public record HelloC2S(int protocolVersion, String clientVersion) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HelloC2S> TYPE = new CustomPacketPayload.Type<>(Protocol.channel("hello"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HelloC2S> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, HelloC2S::protocolVersion,
		ByteBufCodecs.STRING_UTF8, HelloC2S::clientVersion,
		HelloC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
