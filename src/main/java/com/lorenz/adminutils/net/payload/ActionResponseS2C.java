package com.lorenz.adminutils.net.payload;

import com.lorenz.adminutils.net.ErrorCode;
import com.lorenz.adminutils.net.Protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Result of an {@link ActionRequestC2S}, correlated by {@code requestId}.
 */
public record ActionResponseS2C(String requestId, boolean success, String errorCode, String message)
	implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ActionResponseS2C> TYPE =
		new CustomPacketPayload.Type<>(Protocol.channel("action_response"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ActionResponseS2C> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, ActionResponseS2C::requestId,
		ByteBufCodecs.BOOL, ActionResponseS2C::success,
		ByteBufCodecs.STRING_UTF8, ActionResponseS2C::errorCode,
		ByteBufCodecs.STRING_UTF8, ActionResponseS2C::message,
		ActionResponseS2C::new);

	public static ActionResponseS2C ok(String requestId, String message) {
		return new ActionResponseS2C(requestId, true, ErrorCode.OK.id(), message);
	}

	public static ActionResponseS2C error(String requestId, ErrorCode code, String message) {
		return new ActionResponseS2C(requestId, false, code.id(), message);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
