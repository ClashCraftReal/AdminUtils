package com.lorenz.adminutils.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class RateLimiterTest {
	private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

	@Test
	void allowsBurstThenRejects() {
		RateLimiter limiter = new RateLimiter(3, 0.0);
		assertTrue(limiter.tryAcquire(PLAYER));
		assertTrue(limiter.tryAcquire(PLAYER));
		assertTrue(limiter.tryAcquire(PLAYER));
		assertFalse(limiter.tryAcquire(PLAYER));
	}

	@Test
	void tracksPlayersIndependently() {
		RateLimiter limiter = new RateLimiter(1, 0.0);
		UUID other = UUID.fromString("66666666-7777-8888-9999-000000000000");

		assertTrue(limiter.tryAcquire(PLAYER));
		assertFalse(limiter.tryAcquire(PLAYER));
		assertTrue(limiter.tryAcquire(other));
		assertEquals(2, limiter.trackedPlayers());
	}

	@Test
	void forgetReleasesTheBucket() {
		RateLimiter limiter = new RateLimiter(1, 0.0);
		assertTrue(limiter.tryAcquire(PLAYER));
		assertFalse(limiter.tryAcquire(PLAYER));

		limiter.forget(PLAYER);
		assertEquals(0, limiter.trackedPlayers());
		assertTrue(limiter.tryAcquire(PLAYER));
	}

	@Test
	void rejectsNullPlayer() {
		RateLimiter limiter = new RateLimiter(1, 0.0);
		assertFalse(limiter.tryAcquire(null));
		assertEquals(0, limiter.trackedPlayers());
	}

	@Test
	void refillsOverTime() throws InterruptedException {
		RateLimiter limiter = new RateLimiter(1, 100.0);
		assertTrue(limiter.tryAcquire(PLAYER));
		assertFalse(limiter.tryAcquire(PLAYER));

		Thread.sleep(50L);
		assertTrue(limiter.tryAcquire(PLAYER));
	}
}
