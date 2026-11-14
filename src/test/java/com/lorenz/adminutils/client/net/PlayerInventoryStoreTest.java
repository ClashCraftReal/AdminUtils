package com.lorenz.adminutils.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.lorenz.adminutils.net.ErrorCode;
import com.lorenz.adminutils.net.payload.PlayerInventoryChunkS2C;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerInventoryStoreTest {
	private static final UUID ALICE = UUID.fromString("11111111-2222-3333-4444-555555555555");
	private static final UUID BOB = UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaaa");

	/**
	 * The chunk payloads carry {@link ItemStack#EMPTY}, which initialises the item
	 * registry. That registry rejects registration before the vanilla bootstrap, so the
	 * bootstrap has to run before any stack is touched.
	 */
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void startsIdle() {
		PlayerInventoryStore store = new PlayerInventoryStore();

		assertEquals(PlayerInventoryStore.State.IDLE, store.state());
		assertTrue(store.slots().isEmpty());
		assertNull(store.target());
	}

	@Test
	void needsRequestForANewSelection() {
		PlayerInventoryStore store = new PlayerInventoryStore();

		assertTrue(store.needsRequest(ALICE));
		assertFalse(store.needsRequest(null));
	}

	@Test
	void beginRequestEntersLoadingAndStopsFurtherRequests() {
		PlayerInventoryStore store = new PlayerInventoryStore();

		String requestId = store.beginRequest(ALICE);

		assertNotNull(requestId);
		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertEquals(ALICE, store.target());
		assertFalse(store.needsRequest(ALICE));
		assertTrue(store.needsRequest(BOB));
	}

	@Test
	void completesFromAllChunks() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		String requestId = store.beginRequest(ALICE);

		store.onChunk(chunk(requestId, ALICE, 1, 4, 2, 0, 0, 2));
		assertEquals(PlayerInventoryStore.State.LOADING, store.state());

		store.onChunk(chunk(requestId, ALICE, 1, 4, 2, 1, 2, 2));

		assertEquals(PlayerInventoryStore.State.READY, store.state());
		assertEquals(4, store.slots().size());
		assertEquals(1, store.revision());
	}

	@Test
	void ignoresChunksForAnotherRequest() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		store.beginRequest(ALICE);

		store.onChunk(chunk("other-request", ALICE, 1, 4, 1, 0, 0, 4));

		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertTrue(store.slots().isEmpty());
	}

	@Test
	void ignoresChunksForAnotherTarget() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		String requestId = store.beginRequest(ALICE);

		store.onChunk(chunk(requestId, BOB, 1, 4, 1, 0, 0, 4));

		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
	}

	@Test
	void rejectsInconsistentChunkMetadata() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		String requestId = store.beginRequest(ALICE);
		store.onChunk(chunk(requestId, ALICE, 1, 4, 2, 0, 0, 2));

		store.onChunk(chunk(requestId, ALICE, 2, 4, 2, 1, 2, 2));

		assertEquals(PlayerInventoryStore.State.ERROR, store.state());
		assertEquals(ErrorCode.INVALID_REQUEST, store.error());
	}

	@Test
	void rejectsDuplicateChunk() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		String requestId = store.beginRequest(ALICE);
		store.onChunk(chunk(requestId, ALICE, 1, 4, 2, 0, 0, 2));

		store.onChunk(chunk(requestId, ALICE, 1, 4, 2, 0, 0, 2));

		assertEquals(PlayerInventoryStore.State.ERROR, store.state());
		assertEquals(ErrorCode.INVALID_REQUEST, store.error());
	}

	@Test
	void appliesServerErrorForTheOutstandingRequest() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		String requestId = store.beginRequest(ALICE);

		store.onError(requestId, ErrorCode.PERMISSION_DENIED);

		assertEquals(PlayerInventoryStore.State.ERROR, store.state());
		assertEquals(ErrorCode.PERMISSION_DENIED, store.error());
		assertTrue(store.slots().isEmpty());
	}

	@Test
	void ignoresServerErrorForAnotherRequest() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		store.beginRequest(ALICE);

		store.onError("stale-request", ErrorCode.PERMISSION_DENIED);

		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
	}

	@Test
	void doesNotRetryAfterAnError() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		String requestId = store.beginRequest(ALICE);
		store.onError(requestId, ErrorCode.TARGET_UNAVAILABLE);

		// Bounded retry: the failure is retryable until the attempt budget is spent, and a
		// different target is always a fresh request.
		assertTrue(store.needsRequest(ALICE));
		assertTrue(store.needsRequest(BOB));
	}

	@Test
	void retriesAfterATimeoutForTheSameTarget() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		String first = store.beginRequest(ALICE);
		store.onError(first, ErrorCode.TIMEOUT);

		assertEquals(PlayerInventoryStore.State.ERROR, store.state());
		assertTrue(store.needsRequest(ALICE));

		String second = store.beginRequest(ALICE);

		assertNotEquals(first, second);
		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertEquals(ALICE, store.target());
		assertEquals(2, store.attempts());
	}

	@Test
	void stopsRetryingAfterTheAttemptLimit() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);

		for (int attempt = 1; attempt <= PlayerInventoryStore.maxAttempts(); attempt++) {
			String requestId = store.beginRequest(ALICE);
			assertNotNull(requestId);
			assertEquals(attempt, store.attempts());
			store.onError(requestId, ErrorCode.TIMEOUT);
		}

		assertEquals(PlayerInventoryStore.State.ERROR, store.state());
		assertFalse(store.needsRequest(ALICE));
		assertTrue(store.needsRequest(BOB));
	}

	@Test
	void retryIsDelayedSoFailuresDoNotPollPerTick() {
		PlayerInventoryStore store = new PlayerInventoryStore(60_000L);
		String requestId = store.beginRequest(ALICE);
		store.onError(requestId, ErrorCode.TIMEOUT);

		assertFalse(store.needsRequest(ALICE));
	}

	@Test
	void lateChunkAfterTimeoutDoesNotResurrectTheSnapshot() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		String requestId = store.beginRequest(ALICE);
		store.onError(requestId, ErrorCode.TIMEOUT);

		store.onChunk(chunk(requestId, ALICE, 1, 4, 1, 0, 0, 4));

		assertEquals(PlayerInventoryStore.State.ERROR, store.state());
		assertEquals(ErrorCode.TIMEOUT, store.error());
		assertTrue(store.slots().isEmpty());
	}

	@Test
	void changingTargetResetsTheAttemptBudget() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		for (int attempt = 1; attempt <= PlayerInventoryStore.maxAttempts(); attempt++) {
			store.onError(store.beginRequest(ALICE), ErrorCode.TIMEOUT);
		}
		assertFalse(store.needsRequest(ALICE));

		store.beginRequest(BOB);

		assertEquals(1, store.attempts());
		assertEquals(BOB, store.target());
	}

	@Test
	void refreshesOncePerIntervalWhileReady() {
		MutableClock clock = new MutableClock();
		PlayerInventoryStore store = new PlayerInventoryStore(0L, clock);
		completeSnapshot(store, ALICE);

		assertEquals(PlayerInventoryStore.State.READY, store.state());
		assertFalse(store.needsRequest(ALICE));

		clock.advance(PlayerInventoryStore.refreshIntervalMillis() - 1);
		assertFalse(store.needsRequest(ALICE));

		clock.advance(1);
		assertTrue(store.needsRequest(ALICE));

		String requestId = store.beginRequest(ALICE);
		assertNotNull(requestId);
		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertFalse(store.needsRequest(ALICE));
	}

	@Test
	void doesNotRefreshWhileARequestIsLoading() {
		MutableClock clock = new MutableClock();
		PlayerInventoryStore store = new PlayerInventoryStore(0L, clock);
		store.beginRequest(ALICE);

		clock.advance(PlayerInventoryStore.refreshIntervalMillis() * 10L);

		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertFalse(store.needsRequest(ALICE));
	}

	@Test
	void refreshTimingResetsWhenTheTargetChanges() {
		MutableClock clock = new MutableClock();
		PlayerInventoryStore store = new PlayerInventoryStore(0L, clock);
		completeSnapshot(store, ALICE);
		clock.advance(PlayerInventoryStore.refreshIntervalMillis());
		assertTrue(store.needsRequest(ALICE));

		store.beginRequest(BOB);

		assertEquals(BOB, store.target());
		assertEquals(1, store.attempts());
		assertFalse(store.needsRequest(BOB));
	}

	@Test
	void clearStopsRefreshing() {
		MutableClock clock = new MutableClock();
		PlayerInventoryStore store = new PlayerInventoryStore(0L, clock);
		completeSnapshot(store, ALICE);
		clock.advance(PlayerInventoryStore.refreshIntervalMillis());
		assertTrue(store.needsRequest(ALICE));

		store.clear();

		assertEquals(PlayerInventoryStore.State.IDLE, store.state());
		assertNull(store.target());
		// A cleared store has no target, so the selection counts as new rather than as a
		// refresh of the snapshot that was just dropped.
		assertTrue(store.needsRequest(ALICE));
	}

	@Test
	void successfulSnapshotResetsTheRetryBudget() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		for (int attempt = 1; attempt <= PlayerInventoryStore.maxAttempts(); attempt++) {
			store.onError(store.beginRequest(ALICE), ErrorCode.TIMEOUT);
		}
		assertFalse(store.needsRequest(ALICE));

		completeSnapshot(store, ALICE);

		assertEquals(PlayerInventoryStore.State.READY, store.state());
		assertEquals(0, store.attempts());
	}

	@Test
	void keepsTheDisplayedSnapshotWhileARefreshIsLoading() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		completeSnapshot(store, ALICE);
		assertEquals(43, store.slots().size());

		store.beginRequest(ALICE);

		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertTrue(store.hasSnapshot());
		assertEquals(43, store.slots().size());
		assertEquals(1, store.revision());
	}

	@Test
	void replacesTheSnapshotOnlyAfterCompleteAssembly() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		completeSnapshot(store, ALICE);
		assertEquals(1, store.revision());

		String requestId = store.beginRequest(ALICE);
		store.onChunk(chunk(requestId, ALICE, 2, 43, 2, 0, 0, 22));

		// Half of the new snapshot has arrived; the old one is still what is displayed.
		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertEquals(1, store.revision());
		assertEquals(43, store.slots().size());

		store.onChunk(chunk(requestId, ALICE, 2, 43, 2, 1, 22, 21));

		assertEquals(PlayerInventoryStore.State.READY, store.state());
		assertEquals(2, store.revision());
		assertEquals(43, store.slots().size());
	}

	@Test
	void failedRefreshKeepsThePreviousSnapshot() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		completeSnapshot(store, ALICE);

		String requestId = store.beginRequest(ALICE);
		store.onError(requestId, ErrorCode.TIMEOUT);

		assertEquals(PlayerInventoryStore.State.ERROR, store.state());
		assertEquals(ErrorCode.TIMEOUT, store.error());
		assertTrue(store.hasSnapshot());
		assertEquals(43, store.slots().size());
		assertEquals(1, store.revision());
	}

	@Test
	void changingTheSelectionDropsThePreviousSnapshotImmediately() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		completeSnapshot(store, ALICE);
		assertTrue(store.hasSnapshot());

		store.beginRequest(BOB);

		assertEquals(BOB, store.target());
		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertFalse(store.hasSnapshot());
		assertTrue(store.slots().isEmpty());
		assertEquals(0, store.revision());
	}

	@Test
	void staleChunksCannotOverwriteTheDisplayedSnapshot() {
		PlayerInventoryStore store = new PlayerInventoryStore(0L);
		String previousRequest = store.beginRequest(ALICE);
		store.onChunk(chunk(previousRequest, ALICE, 1, 43, 1, 0, 0, 43));
		assertEquals(1, store.revision());

		String currentRequest = store.beginRequest(ALICE);
		store.onChunk(chunk(previousRequest, ALICE, 2, 43, 1, 0, 0, 43));

		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertEquals(1, store.revision());
		assertEquals(43, store.slots().size());

		store.onChunk(chunk(currentRequest, ALICE, 3, 43, 1, 0, 0, 43));

		assertEquals(PlayerInventoryStore.State.READY, store.state());
		assertEquals(3, store.revision());
	}

	/** Drives a complete one-chunk snapshot so the store reaches {@code READY}. */
	private static void completeSnapshot(PlayerInventoryStore store, UUID target) {
		String requestId = store.beginRequest(target);
		store.onChunk(chunk(requestId, target, 1, 43, 1, 0, 0, 43));
	}

	/** Clock the tests advance by hand, so refresh timing needs no sleeping. */
	private static final class MutableClock implements java.util.function.LongSupplier {
		private long millis;

		@Override
		public long getAsLong() {
			return this.millis;
		}

		void advance(long delta) {
			this.millis += delta;
		}
	}

	@Test
	void selectingAnotherTargetDiscardsThePreviousSnapshot() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		String first = store.beginRequest(ALICE);
		store.onChunk(chunk(first, ALICE, 1, 4, 1, 0, 0, 4));
		assertEquals(PlayerInventoryStore.State.READY, store.state());

		String second = store.beginRequest(BOB);

		assertNotEquals(first, second);
		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertEquals(BOB, store.target());
		assertTrue(store.slots().isEmpty());
	}

	@Test
	void staleChunkAfterReselectionIsIgnored() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		String first = store.beginRequest(ALICE);
		String second = store.beginRequest(BOB);

		store.onChunk(chunk(first, ALICE, 1, 4, 1, 0, 0, 4));

		assertEquals(PlayerInventoryStore.State.LOADING, store.state());
		assertEquals(BOB, store.target());
		assertTrue(store.slots().isEmpty());
		assertNotNull(second);
	}

	@Test
	void clearResetsEverything() {
		PlayerInventoryStore store = new PlayerInventoryStore();
		String requestId = store.beginRequest(ALICE);
		store.onChunk(chunk(requestId, ALICE, 1, 4, 1, 0, 0, 4));

		store.clear();

		assertEquals(PlayerInventoryStore.State.IDLE, store.state());
		assertEquals(0, store.attempts());
		assertNull(store.target());
		assertNull(store.requestId());
		assertTrue(store.slots().isEmpty());
	}

	private static PlayerInventoryChunkS2C chunk(String requestId, UUID target, int revision, int slotCount,
		int chunkCount, int chunkIndex, int firstSlot, int size) {
		ItemStack[] stacks = new ItemStack[size];
		java.util.Arrays.fill(stacks, ItemStack.EMPTY);
		return new PlayerInventoryChunkS2C(requestId, target, revision, slotCount, chunkIndex, chunkCount,
			firstSlot, List.of(stacks));
	}
}
