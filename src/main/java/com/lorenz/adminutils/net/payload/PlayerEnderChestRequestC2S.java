package com.lorenz.adminutils.net.payload;

import java.util.UUID;

import com.lorenz.adminutils.net.Protocol;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Asks the server for a read-only snapshot of an online player's Ender Chest.
 *
 * <p>{@code target} is a selector, never an authorization claim: the server resolves the
 * requesting administrator from its own connection context and checks the permission for
 * the operation before touching any player data.
 */
public record PlayerEnderChestRequestC2S(String requestId, UUID target) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PlayerEnderChestRequestC2S> TYPE =
		new CustomPacketPayload.Type<>(Protocol.channel("player_enderchest_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlayerEnderChestRequestC2S> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, PlayerEnderChestRequestC2S::requestId,
		UUIDUtil.STREAM_CODEC, PlayerEnderChestRequestC2S::target,
		PlayerEnderChestRequestC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
