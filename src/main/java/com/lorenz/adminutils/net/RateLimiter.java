package com.lorenz.adminutils.net;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player token bucket. Prevents a modified client from flooding the server with
 * action requests while still allowing short bursts from normal UI interaction.
 */
public final class RateLimiter {
	private final int burst;
	private final double refillPerSecond;
	private final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();

	public RateLimiter(int burst, double refillPerSecond) {
		this.burst = burst;
		this.refillPerSecond = refillPerSecond;
	}

	public boolean tryAcquire(UUID player) {
		if (player == null) {
			return false;
		}
		Bucket bucket = buckets.computeIfAbsent(player, key -> new Bucket(burst));
		return bucket.tryAcquire(burst, refillPerSecond);
	}

	public void forget(UUID player) {
		if (player != null) {
			buckets.remove(player);
		}
	}

	public void clear() {
		buckets.clear();
	}

	public int trackedPlayers() {
		return buckets.size();
	}

	private static final class Bucket {
		private double tokens;
		private long lastRefillNanos;

		private Bucket(int capacity) {
			this.tokens = capacity;
			this.lastRefillNanos = System.nanoTime();
		}

		private synchronized boolean tryAcquire(int capacity, double refillPerSecond) {
			long now = System.nanoTime();
			double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
			lastRefillNanos = now;

			tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
			if (tokens < 1.0) {
				return false;
			}
			tokens -= 1.0;
			return true;
		}
	}
}
