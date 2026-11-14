package com.lorenz.adminutils.net.payload;

import java.util.List;
import java.util.UUID;

import com.lorenz.adminutils.net.Protocol;
import com.lorenz.adminutils.players.PlayerContainerKind;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server-authoritative inventory snapshot. {@code revision} increases on every successful
 * server-side mutation so a client can detect a stale view instead of overwriting newer state.
 *
 * <p>The slot list is bounded to {@link PlayerContainerKind#INVENTORY}'s 43 slots by the
 * collection codec, which rejects a larger declared count before allocating. Stacks keep
 * their vanilla components because the list uses {@link ItemStack#OPTIONAL_STREAM_CODEC}.
 */
public record PlayerInventoryS2C(String requestId, UUID target, int revision, int slotCount, List<ItemStack> slots)
	implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PlayerInventoryS2C> TYPE =
		new CustomPacketPayload.Type<>(Protocol.channel("player_inventory"));

	private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> SLOTS = ByteBufCodecs
		.<RegistryFriendlyByteBuf, ItemStack>list(PlayerContainerKind.INVENTORY.slotCount())
		.apply(ItemStack.OPTIONAL_STREAM_CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, PlayerInventoryS2C> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, PlayerInventoryS2C::requestId,
		UUIDUtil.STREAM_CODEC, PlayerInventoryS2C::target,
		ByteBufCodecs.VAR_INT, PlayerInventoryS2C::revision,
		ByteBufCodecs.VAR_INT, PlayerInventoryS2C::slotCount,
		SLOTS, PlayerInventoryS2C::slots,
		PlayerInventoryS2C::new);

	public PlayerInventoryS2C {
		slots = slots == null ? null : List.copyOf(slots);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
