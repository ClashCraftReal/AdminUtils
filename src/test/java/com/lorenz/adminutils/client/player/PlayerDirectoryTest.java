package com.lorenz.adminutils.client.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerDirectoryTest {
	private static final PlayerListEntry ALICE = entry("Alice");
	private static final PlayerListEntry BOB = entry("Bob");
	private static final PlayerListEntry CAROL = entry("Carol");

	@Test
	void firstUpdateSelectsTheFirstPlayer() {
		PlayerDirectory directory = new PlayerDirectory();

		assertTrue(directory.update(List.of(BOB, ALICE)));

		assertEquals(List.of(ALICE, BOB), directory.entries());
		assertEquals(ALICE.id(), directory.selected());
		assertEquals(ALICE, directory.selectedEntry());
	}

	@Test
	void selectionSurvivesAnUnrelatedJoin() {
		PlayerDirectory directory = new PlayerDirectory();
		directory.update(List.of(ALICE, BOB));
		directory.select(BOB.id());

		directory.update(List.of(ALICE, BOB, CAROL));

		assertEquals(BOB.id(), directory.selected());
		assertEquals(BOB, directory.selectedEntry());
	}

	@Test
	void selectionMovesWhenTheSelectedPlayerLeaves() {
		PlayerDirectory directory = new PlayerDirectory();
		directory.update(List.of(ALICE, BOB, CAROL));
		directory.select(BOB.id());

		directory.update(List.of(ALICE, CAROL));

		assertEquals(ALICE.id(), directory.selected());
		assertEquals(ALICE, directory.selectedEntry());
	}

	@Test
	void selectionClearsWhenTheLastPlayerLeaves() {
		PlayerDirectory directory = new PlayerDirectory();
		directory.update(List.of(ALICE));
		directory.select(ALICE.id());

		directory.update(List.of());

		assertTrue(directory.entries().isEmpty());
		assertNull(directory.selected());
		assertNull(directory.selectedEntry());
	}

	@Test
	void updateReportsWhetherTheListChanged() {
		PlayerDirectory directory = new PlayerDirectory();
		assertTrue(directory.update(List.of(ALICE)));

		assertFalse(directory.update(List.of(ALICE)));
		assertTrue(directory.update(List.of(ALICE, BOB)));
		assertTrue(directory.update(List.of(ALICE)));
	}

	@Test
	void duplicatesCollapseByUuid() {
		PlayerDirectory directory = new PlayerDirectory();
		PlayerListEntry duplicate = new PlayerListEntry(ALICE.id(), "Alice");

		directory.update(List.of(ALICE, duplicate, BOB));

		assertEquals(2, directory.entries().size());
		assertEquals(List.of(ALICE, BOB), directory.entries());
	}

	@Test
	void nullAndEmptySnapshotsAreTreatedAsOffline() {
		PlayerDirectory directory = new PlayerDirectory();
		directory.update(List.of(ALICE));

		assertTrue(directory.update(null));
		assertTrue(directory.entries().isEmpty());
		assertNull(directory.selected());

		assertFalse(directory.update(List.of()));
	}

	@Test
	void entriesAreSortedRegardlessOfSourceOrder() {
		PlayerDirectory directory = new PlayerDirectory();

		directory.update(List.of(CAROL, ALICE, BOB));

		assertEquals(List.of(ALICE, BOB, CAROL), directory.entries());
	}

	@Test
	void clearResetsEntriesAndSelection() {
		PlayerDirectory directory = new PlayerDirectory();
		directory.update(List.of(ALICE, BOB));

		directory.clear();

		assertTrue(directory.entries().isEmpty());
		assertNull(directory.selected());
	}

	private static PlayerListEntry entry(String name) {
		return new PlayerListEntry(UUID.nameUUIDFromBytes(name.getBytes()), name);
	}
}
