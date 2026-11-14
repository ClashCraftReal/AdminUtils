package com.lorenz.adminutils.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventorySnapshotAssemblerTest {
	private static final UUID TARGET = UUID.fromString("11111111-2222-3333-4444-555555555555");
	private static final String REQUEST = "req-1";

	/**
	 * {@link ItemStack#EMPTY} initialises the item registry, which refuses to run before
	 * the vanilla bootstrap. Without this the first stack touch poisons the class
	 * initialiser and every later test in the worker fails with
	 * {@code NoClassDefFoundError}.
	 */
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void completesOnlyWhenEverySlotIsFilled() {
		InventorySnapshotAssembler assembler = started(4, 2);

		assertEquals(InventorySnapshotAssembler.Status.ACCEPTED, assembler.accept(0, 0, slots(2)));
		assertFalse(assembler.isComplete());
		assertNull(assembler.slots());

		assertEquals(InventorySnapshotAssembler.Status.COMPLETE, assembler.accept(1, 2, slots(2)));
		assertTrue(assembler.isComplete());
		assertEquals(4, assembler.slots().size());
	}

	@Test
	void rejectsDuplicateChunkIndex() {
		InventorySnapshotAssembler assembler = started(4, 2);
		assembler.accept(0, 0, slots(2));

		assertEquals(InventorySnapshotAssembler.Status.MISMATCH, assembler.accept(0, 0, slots(2)));
		assertFalse(assembler.isComplete());
	}

	@Test
	void rejectsOverlappingSlotRange() {
		InventorySnapshotAssembler assembler = started(4, 2);
		assembler.accept(0, 0, slots(2));

		assertEquals(InventorySnapshotAssembler.Status.MISMATCH, assembler.accept(1, 1, slots(2)));
		assertFalse(assembler.isComplete());
	}

	@Test
	void rejectsOutOfRangeChunkIndex() {
		InventorySnapshotAssembler assembler = started(4, 2);

		assertEquals(InventorySnapshotAssembler.Status.OUT_OF_RANGE, assembler.accept(2, 0, slots(2)));
		assertEquals(InventorySnapshotAssembler.Status.OUT_OF_RANGE, assembler.accept(-1, 0, slots(2)));
	}

	@Test
	void rejectsSlotRangeBeyondTheSnapshot() {
		InventorySnapshotAssembler assembler = started(4, 2);

		assertEquals(InventorySnapshotAssembler.Status.OUT_OF_RANGE, assembler.accept(0, 3, slots(2)));
		assertFalse(assembler.isComplete());
	}

	@Test
	void rejectsEmptyChunk() {
		InventorySnapshotAssembler assembler = started(4, 2);

		assertEquals(InventorySnapshotAssembler.Status.OUT_OF_RANGE, assembler.accept(0, 0, List.of()));
		assertEquals(InventorySnapshotAssembler.Status.OUT_OF_RANGE, assembler.accept(0, 0, null));
	}

	@Test
	void rejectsChunksBeforeBegin() {
		InventorySnapshotAssembler assembler = new InventorySnapshotAssembler();

		assertEquals(InventorySnapshotAssembler.Status.NOT_STARTED, assembler.accept(0, 0, slots(2)));
		assertFalse(assembler.isComplete());
	}

	@Test
	void beginRejectsUnusableMetadata() {
		InventorySnapshotAssembler assembler = new InventorySnapshotAssembler();

		assertFalse(assembler.begin(null, TARGET, 1, 4, 2));
		assertFalse(assembler.begin(REQUEST, null, 1, 4, 2));
		assertFalse(assembler.begin(REQUEST, TARGET, -1, 4, 2));
		assertFalse(assembler.begin(REQUEST, TARGET, 1, 0, 2));
		assertFalse(assembler.begin(REQUEST, TARGET, 1, 4, 0));
		assertFalse(assembler.begin(REQUEST, TARGET, 1, 4, 9));
		assertFalse(assembler.isStarted());
	}

	@Test
	void beginDiscardsAPreviousAssembly() {
		InventorySnapshotAssembler assembler = started(4, 2);
		assembler.accept(0, 0, slots(2));

		assertTrue(assembler.begin("req-2", TARGET, 9, 4, 2));

		assertEquals("req-2", assembler.requestId());
		assertEquals(9, assembler.revision());
		assertEquals(0, assembler.receivedChunks());
		assertFalse(assembler.isComplete());
	}

	@Test
	void clearDropsEverything() {
		InventorySnapshotAssembler assembler = started(4, 2);
		assembler.accept(0, 0, slots(2));

		assembler.clear();

		assertFalse(assembler.isStarted());
		assertFalse(assembler.isComplete());
		assertNull(assembler.slots());
		assertNull(assembler.requestId());
	}

	private static InventorySnapshotAssembler started(int slotCount, int chunkCount) {
		InventorySnapshotAssembler assembler = new InventorySnapshotAssembler();
		assertTrue(assembler.begin(REQUEST, TARGET, 1, slotCount, chunkCount));
		return assembler;
	}

	private static List<ItemStack> slots(int count) {
		ItemStack[] stacks = new ItemStack[count];
		java.util.Arrays.fill(stacks, ItemStack.EMPTY);
		return List.of(stacks);
	}
}
