package com.lorenz.adminutils.client.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Resolves and caches the skin texture of an online player.
 *
 * <p>The texture identifier comes from the client tab list
 * ({@code PlayerInfo.getSkin().body().texturePath()}), which is the same source the
 * vanilla tab overlay uses. No external skin service is contacted.
 *
 * <p>Successful lookups are cached by UUID so the render loop never re-resolves the
 * same skin. Failures are cached for a short retry window instead of being retried
 * every frame: a skin that is still downloading becomes available shortly after, and
 * a permanently unavailable one costs at most one lookup per second.
 */
public final class SkinTextureCache {
	private static final long RETRY_MILLIS = 1000L;

	private final Map<UUID, Entry> cache = new HashMap<>();

	/**
	 * Texture for the player, or null when the skin is not available yet. Callers draw
	 * a fallback in that case rather than skipping the row.
	 */
	public Identifier textureFor(UUID id) {
		if (id == null) {
			return null;
		}
		long now = System.currentTimeMillis();
		Entry cached = this.cache.get(id);
		if (cached != null && (cached.texture() != null || now < cached.retryAtMillis())) {
			return cached.texture();
		}

		Identifier resolved = resolve(id);
		this.cache.put(id, new Entry(resolved, now + RETRY_MILLIS));
		return resolved;
	}

	/**
	 * Drops entries for players who are no longer online so the cache cannot grow
	 * across a long session.
	 */
	public void retain(Set<UUID> online) {
		this.cache.keySet().retainAll(online);
	}

	public void clear() {
		this.cache.clear();
	}

	public int size() {
		return this.cache.size();
	}

	private static Identifier resolve(UUID id) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null) {
			return null;
		}
		PlayerInfo info = connection.getPlayerInfo(id);
		if (info == null) {
			return null;
		}
		PlayerSkin skin = info.getSkin();
		if (skin == null || skin.body() == null) {
			return null;
		}
		return skin.body().texturePath();
	}

	private record Entry(Identifier texture, long retryAtMillis) {
	}
}
