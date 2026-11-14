package com.lorenz.adminutils.client.player;

import java.util.UUID;

/**
 * One entry of the client's online player list.
 *
 * <p>Deliberately free of Minecraft types so filtering, sorting and selection can be
 * unit tested without a game client. The skin texture is resolved separately by
 * {@link SkinTextureCache}, keyed on {@link #id()}.
 */
public record PlayerListEntry(UUID id, String name) implements Comparable<PlayerListEntry> {
	public PlayerListEntry {
		if (id == null) {
			throw new IllegalArgumentException("id");
		}
		name = name == null ? "" : name;
	}

	/**
	 * Case-insensitive name order, falling back to the UUID so two players sharing a
	 * display name keep a stable position.
	 */
	@Override
	public int compareTo(PlayerListEntry other) {
		int byName = String.CASE_INSENSITIVE_ORDER.compare(this.name, other.name);
		return byName != 0 ? byName : this.id.compareTo(other.id);
	}
}
