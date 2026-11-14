package com.lorenz.adminutils.client.net;

import java.util.List;
import java.util.UUID;

import net.minecraft.world.item.ItemStack;

/**
 * Reassembles a chunked inventory snapshot.
 *
 * <p>Chunks are only accepted for the request that started the assembly. Duplicate
 * indices, overlapping slot ranges and out-of-range indices are rejected, so a snapshot
 * can never be completed from a mixture of two responses or from a replayed chunk.
 *
 * <p>{@link #isComplete()} is only true once every slot has been filled by an accepted
 * chunk. An incomplete assembly never exposes a slot list.
 */
public final class InventorySnapshotAssembler {
	public enum Status {
		ACCEPTED,
		COMPLETE,
		DUPLICATE,
		OUT_OF_RANGE,
		MISMATCH,
		NOT_STARTED
	}

	private String requestId;
	private UUID target;
	private int revision;
	private int slotCount;
	private int chunkCount;
	private ItemStack[] slots;
	private boolean[] received;
	private int receivedCount;

	/**
	 * Starts a new assembly, discarding any previous one.
	 *
	 * @return false when the metadata is not a usable snapshot shape
	 */
	public boolean begin(String requestId, UUID target, int revision, int slotCount, int chunkCount) {
		clear();
		if (requestId == null || target == null || revision < 0 || slotCount <= 0
			|| chunkCount < 1 || chunkCount > slotCount) {
			return false;
		}
		this.requestId = requestId;
		this.target = target;
		this.revision = revision;
		this.slotCount = slotCount;
		this.chunkCount = chunkCount;
		this.slots = new ItemStack[slotCount];
		this.received = new boolean[slotCount];
		return true;
	}

	public boolean isStarted() {
		return this.slots != null;
	}

	public String requestId() {
		return this.requestId;
	}

	public UUID target() {
		return this.target;
	}

	public int revision() {
		return this.revision;
	}

	public int slotCount() {
		return this.slotCount;
	}

	public int chunkCount() {
		return this.chunkCount;
	}

	public int receivedChunks() {
		return this.receivedCount;
	}

	public Status accept(int chunkIndex, int firstSlot, List<ItemStack> chunkSlots) {
		if (this.slots == null) {
			return Status.NOT_STARTED;
		}
		if (chunkIndex < 0 || chunkIndex >= this.chunkCount) {
			return Status.OUT_OF_RANGE;
		}
		if (chunkSlots == null || chunkSlots.isEmpty() || firstSlot < 0) {
			return Status.OUT_OF_RANGE;
		}
		int end = firstSlot + chunkSlots.size();
		if (end > this.slotCount) {
			return Status.OUT_OF_RANGE;
		}
		for (int slot = firstSlot; slot < end; slot++) {
			if (this.received[slot]) {
				return Status.MISMATCH;
			}
		}
		for (ItemStack stack : chunkSlots) {
			if (stack == null) {
				return Status.MISMATCH;
			}
		}

		for (int offset = 0; offset < chunkSlots.size(); offset++) {
			this.slots[firstSlot + offset] = chunkSlots.get(offset);
			this.received[firstSlot + offset] = true;
		}
		this.receivedCount++;
		return isComplete() ? Status.COMPLETE : Status.ACCEPTED;
	}

	public boolean isComplete() {
		if (this.slots == null || this.receivedCount < this.chunkCount) {
			return false;
		}
		for (boolean filled : this.received) {
			if (!filled) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Immutable slot list, or null while the snapshot is incomplete.
	 */
	public List<ItemStack> slots() {
		return isComplete() ? List.of(this.slots) : null;
	}

	public void clear() {
		this.requestId = null;
		this.target = null;
		this.revision = 0;
		this.slotCount = 0;
		this.chunkCount = 0;
		this.slots = null;
		this.received = null;
		this.receivedCount = 0;
	}
}
