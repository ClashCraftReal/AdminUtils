package com.lorenz.adminutils.client.player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical online player list plus the current selection.
 *
 * <p>Pure state: {@link #update} takes the latest snapshot and reports whether it
 * changed, so the page can skip work on the ticks where nothing joined or left. The
 * Minecraft-facing read lives in the page, which keeps this class testable.
 */
public final class PlayerDirectory {
	private List<PlayerListEntry> entries = List.of();
	private UUID selected;

	public List<PlayerListEntry> entries() {
		return entries;
	}

	public UUID selected() {
		return this.selected;
	}

	public PlayerListEntry selectedEntry() {
		if (this.selected == null) {
			return null;
		}
		for (PlayerListEntry entry : this.entries) {
			if (entry.id().equals(this.selected)) {
				return entry;
			}
		}
		return null;
	}

	public void select(UUID id) {
		this.selected = id;
	}

	/**
	 * Replaces the directory with the latest snapshot.
	 *
	 * <p>Duplicates are collapsed by UUID, keeping the first occurrence, so a source
	 * that reports the same player twice cannot produce two rows. The selection is
	 * preserved while that player is still online; otherwise the first remaining
	 * player is selected, and an empty list clears the selection.
	 *
	 * @return true when the entry list differs from the previous one
	 */
	public boolean update(List<PlayerListEntry> latest) {
		Map<UUID, PlayerListEntry> unique = new LinkedHashMap<>();
		if (latest != null) {
			for (PlayerListEntry entry : latest) {
				if (entry != null) {
					unique.putIfAbsent(entry.id(), entry);
				}
			}
		}

		List<PlayerListEntry> next = new ArrayList<>(unique.values());
		next.sort(PlayerListEntry::compareTo);

		boolean changed = !next.equals(this.entries);
		this.entries = List.copyOf(next);

		if (this.selected != null && !unique.containsKey(this.selected)) {
			this.selected = this.entries.isEmpty() ? null : this.entries.get(0).id();
		} else if (this.selected == null && !this.entries.isEmpty()) {
			this.selected = this.entries.get(0).id();
		}

		return changed;
	}

	public void clear() {
		this.entries = List.of();
		this.selected = null;
	}
}
