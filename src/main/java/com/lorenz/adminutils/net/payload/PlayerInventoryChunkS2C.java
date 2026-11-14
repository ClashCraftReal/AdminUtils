package com.lorenz.adminutils.net.payload;

import java.util.List;
import java.util.UUID;

import com.lorenz.adminutils.net.Protocol;
import com.lorenz.adminutils.players.InventorySnapshotLimits;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * One bounded slice of a player inventory snapshot.
 *
 * <p>A 43-slot inventory can exceed the clientbound custom payload cap, so a snapshot is
 * transferred as {@code chunkCount} chunks that each carry a contiguous slot range.
 * {@code firstSlot} plus the slot list length must stay within {@code slotCount}.
 *
 * <p>{@code slotCount} and {@code revision} are repeated on every chunk so a chunk can be
 * validated on its own, before it is offered to the assembler.
 */
public record PlayerInventoryChunkS2C(String requestId, UUID target, int revision, int slotCount,
	int chunkIndex, int chunkCount, int firstSlot, List<ItemStack> slots) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PlayerInventoryChunkS2C> TYPE =
		new CustomPacketPayload.Type<>(Protocol.channel("player_inventory_chunk"));

	private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> SLOTS = ByteBufCodecs
		.<RegistryFriendlyByteBuf, ItemStack>list(InventorySnapshotLimits.MAX_SLOTS)
		.apply(ItemStack.OPTIONAL_STREAM_CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, PlayerInventoryChunkS2C> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, PlayerInventoryChunkS2C::requestId,
		UUIDUtil.STREAM_CODEC, PlayerInventoryChunkS2C::target,
		ByteBufCodecs.VAR_INT, PlayerInventoryChunkS2C::revision,
		ByteBufCodecs.VAR_INT, PlayerInventoryChunkS2C::slotCount,
		ByteBufCodecs.VAR_INT, PlayerInventoryChunkS2C::chunkIndex,
		ByteBufCodecs.VAR_INT, PlayerInventoryChunkS2C::chunkCount,
		ByteBufCodecs.VAR_INT, PlayerInventoryChunkS2C::firstSlot,
		SLOTS, PlayerInventoryChunkS2C::slots,
		PlayerInventoryChunkS2C::new);

	public PlayerInventoryChunkS2C {
		slots = slots == null ? null : List.copyOf(slots);
	}

	public int endSlot() {
		return this.firstSlot + (this.slots == null ? 0 : this.slots.size());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
