package com.lorenz.adminutils.players;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.lorenz.adminutils.net.payload.PlayerInventoryChunkS2C;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * Measures a slot list with the real {@code ItemStack} codec and turns it into bounded
 * chunks.
 *
 * <p>Each slot is encoded on its own into a scratch buffer so the byte count reflects the
 * actual component payload, not an estimate. The scratch buffer is reused and cleared
 * between slots, so measuring does not allocate per slot.
 *
 * <p>A snapshot is either fully planned or rejected. Nothing is truncated and no
 * components are stripped.
 */
public final class PlayerInventorySnapshotFactory {
	public record Result(List<PlayerInventoryChunkS2C> chunks, InventorySnapshotChunker.Rejection rejection) {
		public Result {
			chunks = chunks == null ? List.of() : List.copyOf(chunks);
		}

		public boolean rejected() {
			return this.rejection != null;
		}

		public static Result of(List<PlayerInventoryChunkS2C> chunks) {
			return new Result(chunks, null);
		}

		public static Result reject(InventorySnapshotChunker.Rejection rejection) {
			return new Result(List.of(), rejection);
		}
	}

	private PlayerInventorySnapshotFactory() {
	}

	/**
	 * @param slots immutable copies taken from the target's live inventory
	 */
	public static Result build(String requestId, UUID target, int revision, List<ItemStack> slots,
		RegistryAccess registries) {
		if (slots == null || slots.isEmpty() || registries == null) {
			return Result.reject(InventorySnapshotChunker.Rejection.INVALID_INPUT);
		}

		int[] sizes = measure(slots, registries);
		InventorySnapshotChunker.Plan plan = InventorySnapshotChunker.plan(sizes);
		if (plan.rejected()) {
			return Result.reject(plan.rejection());
		}

		List<PlayerInventoryChunkS2C> chunks = new ArrayList<>(plan.chunks().size());
		for (int index = 0; index < plan.chunks().size(); index++) {
			InventorySnapshotChunker.Chunk chunk = plan.chunks().get(index);
			List<ItemStack> slice = List.copyOf(slots.subList(chunk.firstSlot(), chunk.endSlot()));
			chunks.add(new PlayerInventoryChunkS2C(requestId, target, revision, slots.size(),
				index, plan.chunks().size(), chunk.firstSlot(), slice));
		}
		return Result.of(chunks);
	}

	private static int[] measure(List<ItemStack> slots, RegistryAccess registries) {
		int[] sizes = new int[slots.size()];
		RegistryFriendlyByteBuf scratch = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try {
			for (int index = 0; index < slots.size(); index++) {
				ItemStack stack = slots.get(index);
				scratch.clear();
				ItemStack.OPTIONAL_STREAM_CODEC.encode(scratch, stack == null ? ItemStack.EMPTY : stack);
				sizes[index] = scratch.readableBytes();
			}
		} finally {
			scratch.release();
		}
		return sizes;
	}
}
