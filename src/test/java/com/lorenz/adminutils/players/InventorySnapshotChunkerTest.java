package com.lorenz.adminutils.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class InventorySnapshotChunkerTest {
	@Test
	void singleChunkWhenEverythingFits() {
		int[] sizes = new int[43];
		java.util.Arrays.fill(sizes, 10);

		InventorySnapshotChunker.Plan plan = InventorySnapshotChunker.plan(sizes);

		assertFalse(plan.rejected());
		assertEquals(1, plan.chunks().size());
		assertEquals(new InventorySnapshotChunker.Chunk(0, 43), plan.chunks().get(0));
	}

	@Test
	void splitsWhenTheChunkBudgetIsExceeded() {
		int[] sizes = new int[43];
		java.util.Arrays.fill(sizes, 100);

		InventorySnapshotChunker.Plan plan = InventorySnapshotChunker.plan(sizes, 400, 100_000, 16);

		assertFalse(plan.rejected());
		assertTrue(plan.chunks().size() > 1);
		assertCoversEverySlotOnce(plan.chunks(), 43);
	}

	@Test
	void chunksAreContiguousAndOrdered() {
		int[] sizes = new int[43];
		java.util.Arrays.fill(sizes, 60);

		InventorySnapshotChunker.Plan plan = InventorySnapshotChunker.plan(sizes, 200, 100_000, 16);

		assertFalse(plan.rejected());
		int expectedStart = 0;
		for (InventorySnapshotChunker.Chunk chunk : plan.chunks()) {
			assertEquals(expectedStart, chunk.firstSlot());
			assertTrue(chunk.slotCount() > 0);
			expectedStart = chunk.endSlot();
		}
		assertEquals(43, expectedStart);
	}

	@Test
	void rejectsASlotLargerThanTheChunkBudget() {
		int[] sizes = new int[43];
		sizes[7] = 500;

		InventorySnapshotChunker.Plan plan = InventorySnapshotChunker.plan(sizes, 200, 100_000, 16);

		assertTrue(plan.rejected());
		assertEquals(InventorySnapshotChunker.Rejection.SLOT_TOO_LARGE, plan.rejection());
		assertTrue(plan.chunks().isEmpty());
	}

	@Test
	void rejectsASnapshotLargerThanTheTotalBudget() {
		int[] sizes = new int[43];
		java.util.Arrays.fill(sizes, 1000);

		InventorySnapshotChunker.Plan plan = InventorySnapshotChunker.plan(sizes, 200_000, 5000, 16);

		assertTrue(plan.rejected());
		assertEquals(InventorySnapshotChunker.Rejection.SNAPSHOT_TOO_LARGE, plan.rejection());
		assertTrue(plan.chunks().isEmpty());
	}

	@Test
	void rejectsTooManyChunks() {
		int[] sizes = new int[43];
		java.util.Arrays.fill(sizes, 100);

		InventorySnapshotChunker.Plan plan = InventorySnapshotChunker.plan(sizes, 100, 100_000, 2);

		assertTrue(plan.rejected());
		assertEquals(InventorySnapshotChunker.Rejection.TOO_MANY_CHUNKS, plan.rejection());
	}

	@Test
	void rejectsInvalidInput() {
		assertEquals(InventorySnapshotChunker.Rejection.INVALID_INPUT,
			InventorySnapshotChunker.plan(null).rejection());
		assertEquals(InventorySnapshotChunker.Rejection.INVALID_INPUT,
			InventorySnapshotChunker.plan(new int[0]).rejection());

		int[] negative = new int[43];
		negative[3] = -1;
		assertEquals(InventorySnapshotChunker.Rejection.INVALID_INPUT,
			InventorySnapshotChunker.plan(negative).rejection());
	}

	@Test
	void defaultPlanUsesTheSharedLimits() {
		int[] sizes = new int[43];
		java.util.Arrays.fill(sizes, 1);

		InventorySnapshotChunker.Plan plan = InventorySnapshotChunker.plan(sizes);

		assertFalse(plan.rejected());
		assertTrue(plan.chunks().size() <= InventorySnapshotLimits.MAX_CHUNKS);
	}

	@Test
	void limitsAreBelowTheClientboundPayloadCap() {
		assertTrue(InventorySnapshotLimits.MAX_SNAPSHOT_BYTES < 1_048_576);
		assertTrue(InventorySnapshotLimits.MAX_CHUNK_BYTES < InventorySnapshotLimits.MAX_SNAPSHOT_BYTES);
		assertEquals(43, InventorySnapshotLimits.MAX_SLOTS);
	}

	private static void assertCoversEverySlotOnce(List<InventorySnapshotChunker.Chunk> chunks, int slotCount) {
		boolean[] covered = new boolean[slotCount];
		for (InventorySnapshotChunker.Chunk chunk : chunks) {
			for (int slot = chunk.firstSlot(); slot < chunk.endSlot(); slot++) {
				assertFalse(covered[slot], "slot covered twice: " + slot);
				covered[slot] = true;
			}
		}
		for (boolean value : covered) {
			assertTrue(value);
		}
	}
}
