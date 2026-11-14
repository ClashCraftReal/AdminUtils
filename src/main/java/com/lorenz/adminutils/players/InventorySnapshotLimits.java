package com.lorenz.adminutils.players;

/**
 * Hard bounds for a player container snapshot on the wire.
 *
 * <p>{@code ClientboundCustomPayloadPacket.MAX_PAYLOAD_SIZE} is 1048576 bytes and the
 * frame length prefix is a 3-byte varint, so the payload cap is the binding limit. The
 * snapshot budget is kept below it with headroom for the packet header, the payload type
 * identifier and the surrounding frame.
 *
 * <p>A 43-slot inventory is not guaranteed to fit in one payload: a single shulker box
 * holding written books can exceed the cap on its own. Snapshots are therefore split into
 * chunks, each independently bounded, with a bounded chunk count.
 */
public final class InventorySnapshotLimits {
	/** Total encoded budget for one complete snapshot, across all chunks. */
	public static final int MAX_SNAPSHOT_BYTES = 900_000;

	/** Encoded budget for a single chunk. */
	public static final int MAX_CHUNK_BYTES = 200_000;

	/** Upper bound on chunks per snapshot. */
	public static final int MAX_CHUNKS = 16;

	/** Slots in a full inventory snapshot. */
	public static final int MAX_SLOTS = PlayerContainerKind.INVENTORY.slotCount();

	private InventorySnapshotLimits() {
	}
}
