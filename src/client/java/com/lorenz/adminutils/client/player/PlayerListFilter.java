package com.lorenz.adminutils.client.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Search and sort for the online player list. Pure functions over
 * {@link PlayerListEntry} so the behaviour is testable without a client.
 */
public final class PlayerListFilter {
	public enum SortOrder {
		ASCENDING,
		DESCENDING;

		public SortOrder toggled() {
			return this == ASCENDING ? DESCENDING : ASCENDING;
		}

		public String label() {
			return this == ASCENDING ? "A-Z" : "Z-A";
		}
	}

	private PlayerListFilter() {
	}

	/**
	 * Case-insensitive substring match on the player name. A null or blank query
	 * matches every entry, so clearing the search restores the full list.
	 */
	public static boolean matches(PlayerListEntry entry, String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		return entry.name().toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
	}

	/**
	 * Filters and sorts in one pass. The input list is never modified, so the caller
	 * keeps its canonical directory order.
	 */
	public static List<PlayerListEntry> apply(List<PlayerListEntry> entries, String query, SortOrder order) {
		List<PlayerListEntry> result = new ArrayList<>(entries.size());
		for (PlayerListEntry entry : entries) {
			if (matches(entry, query)) {
				result.add(entry);
			}
		}
		result.sort(PlayerListEntry::compareTo);
		if (order == SortOrder.DESCENDING) {
			Collections.reverse(result);
		}
		return result;
	}
}
