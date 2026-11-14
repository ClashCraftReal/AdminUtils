package com.lorenz.adminutils.client.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerListFilterTest {
	private static final PlayerListEntry ALICE = entry("Alice");
	private static final PlayerListEntry BOB = entry("Bob");
	private static final PlayerListEntry CAROL = entry("carol");

	@Test
	void blankQueryMatchesEverything() {
		assertTrue(PlayerListFilter.matches(ALICE, null));
		assertTrue(PlayerListFilter.matches(ALICE, ""));
		assertTrue(PlayerListFilter.matches(ALICE, "   "));
		assertEquals(3, PlayerListFilter.apply(List.of(ALICE, BOB, CAROL), "", sort()).size());
	}

	@Test
	void searchIsCaseInsensitive() {
		assertTrue(PlayerListFilter.matches(ALICE, "alice"));
		assertTrue(PlayerListFilter.matches(ALICE, "ALICE"));
		assertTrue(PlayerListFilter.matches(ALICE, "lic"));
		assertTrue(PlayerListFilter.matches(CAROL, "CAR"));
		assertFalse(PlayerListFilter.matches(BOB, "alice"));
	}

	@Test
	void searchIgnoresSurroundingWhitespace() {
		assertTrue(PlayerListFilter.matches(ALICE, "  ali  "));
	}

	@Test
	void emptyResultWhenNothingMatches() {
		assertTrue(PlayerListFilter.apply(List.of(ALICE, BOB), "zzz", sort()).isEmpty());
	}

	@Test
	void sortsAlphabeticallyAscending() {
		List<PlayerListEntry> sorted = PlayerListFilter.apply(List.of(CAROL, BOB, ALICE), "",
			PlayerListFilter.SortOrder.ASCENDING);
		assertEquals(List.of(ALICE, BOB, CAROL), sorted);
	}

	@Test
	void sortsAlphabeticallyDescending() {
		List<PlayerListEntry> sorted = PlayerListFilter.apply(List.of(ALICE, BOB, CAROL), "",
			PlayerListFilter.SortOrder.DESCENDING);
		assertEquals(List.of(CAROL, BOB, ALICE), sorted);
	}

	@Test
	void sortingIsCaseInsensitive() {
		List<PlayerListEntry> sorted = PlayerListFilter.apply(List.of(CAROL, BOB, ALICE), "",
			PlayerListFilter.SortOrder.ASCENDING);
		assertEquals("Alice", sorted.get(0).name());
		assertEquals("carol", sorted.get(2).name());
	}

	@Test
	void filterAndSortCombine() {
		List<PlayerListEntry> sorted = PlayerListFilter.apply(List.of(CAROL, BOB, ALICE), "o",
			PlayerListFilter.SortOrder.DESCENDING);
		assertEquals(List.of(CAROL, BOB), sorted);
	}

	@Test
	void sortOrderToggles() {
		assertEquals(PlayerListFilter.SortOrder.DESCENDING, PlayerListFilter.SortOrder.ASCENDING.toggled());
		assertEquals(PlayerListFilter.SortOrder.ASCENDING, PlayerListFilter.SortOrder.DESCENDING.toggled());
		assertEquals("A-Z", PlayerListFilter.SortOrder.ASCENDING.label());
		assertEquals("Z-A", PlayerListFilter.SortOrder.DESCENDING.label());
	}

	@Test
	void applyDoesNotMutateInput() {
		List<PlayerListEntry> input = List.of(CAROL, BOB, ALICE);
		PlayerListFilter.apply(input, "", PlayerListFilter.SortOrder.DESCENDING);
		assertEquals(List.of(CAROL, BOB, ALICE), input);
	}

	private static PlayerListFilter.SortOrder sort() {
		return PlayerListFilter.SortOrder.ASCENDING;
	}

	private static PlayerListEntry entry(String name) {
		return new PlayerListEntry(UUID.nameUUIDFromBytes(name.getBytes()), name);
	}
}
