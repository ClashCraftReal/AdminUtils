package com.lorenz.adminutils.players;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a slot list into bounded chunks using the encoded size of each slot.
 *
 * <p>Pure integer arithmetic so the packing rules can be tested without a codec or a
 * live registry. The caller measures each slot with the real {@code ItemStack} codec and
 * passes the byte counts in.
 *
 * <p>Chunks are contiguous and cover every slot exactly once, in order. A snapshot is
 * either planned completely or rejected: there is no partial plan, so a truncated
 * snapshot can never be sent as if it were whole.
 */
public final class InventorySnapshotChunker {
	public enum Rejection {
		/** A single slot exceeds the per-chunk budget and cannot be split. */
		SLOT_TOO_LARGE,
		/** The whole snapshot exceeds the total budget. */
		SNAPSHOT_TOO_LARGE,
		/** The plan needs more chunks than the protocol allows. */
		TOO_MANY_CHUNKS,
		/** The input was null, empty or contained a negative size. */
		INVALID_INPUT
	}

	/** Half-open slot range {@code [firstSlot, firstSlot + slotCount)}. */
	public record Chunk(int firstSlot, int slotCount) {
		public Chunk {
			if (firstSlot < 0 || slotCount <= 0) {
				throw new IllegalArgumentException("chunk range");
			}
		}

		public int endSlot() {
			return this.firstSlot + this.slotCount;
		}
	}

	public record Plan(List<Chunk> chunks, Rejection rejection) {
		public Plan {
			chunks = chunks == null ? List.of() : List.copyOf(chunks);
		}

		public boolean rejected() {
			return this.rejection != null;
		}

		public static Plan of(List<Chunk> chunks) {
			return new Plan(chunks, null);
		}

		public static Plan reject(Rejection rejection) {
			return new Plan(List.of(), rejection);
		}
	}

	private InventorySnapshotChunker() {
	}

	public static Plan plan(int[] slotSizes) {
		return plan(slotSizes, InventorySnapshotLimits.MAX_CHUNK_BYTES,
			InventorySnapshotLimits.MAX_SNAPSHOT_BYTES, InventorySnapshotLimits.MAX_CHUNKS);
	}

	public static Plan plan(int[] slotSizes, int maxChunkBytes, int maxSnapshotBytes, int maxChunks) {
		if (slotSizes == null || slotSizes.length == 0) {
			return Plan.reject(Rejection.INVALID_INPUT);
		}

		long total = 0;
		for (int size : slotSizes) {
			if (size < 0) {
				return Plan.reject(Rejection.INVALID_INPUT);
			}
			total += size;
		}
		if (total > maxSnapshotBytes) {
			return Plan.reject(Rejection.SNAPSHOT_TOO_LARGE);
		}

		List<Chunk> chunks = new ArrayList<>();
		int start = 0;
		long current = 0;
		for (int index = 0; index < slotSizes.length; index++) {
			int size = slotSizes[index];
			if (size > maxChunkBytes) {
				return Plan.reject(Rejection.SLOT_TOO_LARGE);
			}
			if (current > 0 && current + size > maxChunkBytes) {
				chunks.add(new Chunk(start, index - start));
				start = index;
				current = 0;
			}
			current += size;
		}
		chunks.add(new Chunk(start, slotSizes.length - start));

		if (chunks.size() > maxChunks) {
			return Plan.reject(Rejection.TOO_MANY_CHUNKS);
		}
		return Plan.of(chunks);
	}
}
