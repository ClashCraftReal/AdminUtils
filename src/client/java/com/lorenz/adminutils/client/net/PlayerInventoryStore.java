package com.lorenz.adminutils.client.net;

import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;

import com.lorenz.adminutils.net.ErrorCode;
import com.lorenz.adminutils.net.payload.PlayerInventoryChunkS2C;

import net.minecraft.world.item.ItemStack;

/**
 * Client-side state for the read-only inventory view.
 *
 * <p>Holds at most one snapshot, always for the currently selected target. A response is
 * only applied when its request identifier matches the outstanding request, so a late
 * reply for a previous selection cannot replace the current one.
 *
 * <p>Receiving data is never treated as proof of authorization: the server re-checks the
 * permission on every request, and this class only renders what the server sent.
 */
public final class PlayerInventoryStore {
	public enum State {
		IDLE,
		LOADING,
		READY,
		ERROR
	}

	private static final long TIMEOUT_MILLIS = 5000L;
	/** Attempts allowed per selected target, including the first one. */
	private static final int MAX_ATTEMPTS = 3;
	/** Pause before a failed request may be retried, so retries never run per tick. */
	private static final long RETRY_DELAY_MILLIS = 1000L;
	/** Cadence of the automatic refresh while a snapshot is already displayed. */
	private static final long REFRESH_INTERVAL_MILLIS = 1000L;
	private static final PlayerInventoryStore SHARED = new PlayerInventoryStore();

	/**
	 * Process-wide store used by the Players page and the client receivers. Only one
	 * Players screen exists at a time, so a single instance keeps the receiver and the
	 * page in sync without a listener registry.
	 */
	public static PlayerInventoryStore shared() {
		return SHARED;
	}

	private final InventorySnapshotAssembler assembler = new InventorySnapshotAssembler();
	private final long retryDelayMillis;
	private final LongSupplier clock;

	private UUID target;
	private String requestId;
	private State state = State.IDLE;
	private ErrorCode error;
	private List<ItemStack> slots = List.of();
	private int revision;
	private long deadlineMillis;
	private int attempts;
	private long retryAfterMillis;
	private long refreshDueMillis;

	public PlayerInventoryStore() {
		this(RETRY_DELAY_MILLIS, System::currentTimeMillis);
	}

	/**
	 * @param retryDelayMillis pause before a failed request may be retried; tests pass 0
	 */
	PlayerInventoryStore(long retryDelayMillis) {
		this(retryDelayMillis, System::currentTimeMillis);
	}

	/**
	 * @param retryDelayMillis pause before a failed request may be retried
	 * @param clock            time source, so refresh timing can be tested without sleeping
	 */
	PlayerInventoryStore(long retryDelayMillis, LongSupplier clock) {
		this.retryDelayMillis = Math.max(0L, retryDelayMillis);
		this.clock = clock;
	}

	private long now() {
		return this.clock.getAsLong();
	}

	public State state() {
		return this.state;
	}

	public ErrorCode error() {
		return this.error;
	}

	public List<ItemStack> slots() {
		return this.slots;
	}

	/**
	 * True when a complete snapshot is available for the current target. The snapshot
	 * survives a background refresh, so the view keeps drawing it while a newer one is
	 * being assembled.
	 */
	public boolean hasSnapshot() {
		return !this.slots.isEmpty();
	}

	public UUID target() {
		return this.target;
	}

	public int revision() {
		return this.revision;
	}

	public String requestId() {
		return this.requestId;
	}

	/** Attempts already spent on the current target, including the outstanding one. */
	public int attempts() {
		return this.attempts;
	}

	public static int maxAttempts() {
		return MAX_ATTEMPTS;
	}

	public static long refreshIntervalMillis() {
		return REFRESH_INTERVAL_MILLIS;
	}

	/**
	 * True when a request should be sent for the selection.
	 *
	 * <p>A changed selection always needs a request. A displayed snapshot is refreshed once
	 * per {@link #REFRESH_INTERVAL_MILLIS}. A failed request is retried, but only after
	 * {@code retryDelayMillis} and only while fewer than {@link #MAX_ATTEMPTS} consecutive
	 * attempts have failed, so neither path can become a per-tick poll loop. A request that
	 * is still {@code LOADING} is never overlapped.
	 */
	public boolean needsRequest(UUID selected) {
		if (selected == null) {
			return false;
		}
		if (!selected.equals(this.target)) {
			return true;
		}
		return switch (this.state) {
			case IDLE -> true;
			case READY -> now() >= this.refreshDueMillis;
			case ERROR -> this.attempts < MAX_ATTEMPTS && now() >= this.retryAfterMillis;
			case LOADING -> false;
		};
	}

	/**
	 * Starts a request for the given target and returns the identifier to send.
	 *
	 * <p>The last complete snapshot is kept when the target is unchanged, so a background
	 * refresh never blanks the view. A new target drops it immediately, because the previous
	 * player's items must not be shown for the new selection.
	 */
	public String beginRequest(UUID selected) {
		if (selected == null) {
			clear();
			return null;
		}
		if (!selected.equals(this.target)) {
			this.attempts = 0;
			this.slots = List.of();
			this.revision = 0;
		}
		this.attempts++;
		this.target = selected;
		this.requestId = UUID.randomUUID().toString();
		this.state = State.LOADING;
		this.error = null;
		this.retryAfterMillis = 0L;
		this.deadlineMillis = now() + TIMEOUT_MILLIS;
		this.refreshDueMillis = now() + REFRESH_INTERVAL_MILLIS;
		this.assembler.clear();
		return this.requestId;
	}

	public void onChunk(PlayerInventoryChunkS2C payload) {
		if (payload == null || this.state != State.LOADING) {
			return;
		}
		if (this.requestId == null || !this.requestId.equals(payload.requestId())) {
			return;
		}
		if (this.target == null || !this.target.equals(payload.target())) {
			return;
		}

		if (!this.assembler.isStarted()) {
			if (!this.assembler.begin(payload.requestId(), payload.target(), payload.revision(),
				payload.slotCount(), payload.chunkCount())) {
				fail(ErrorCode.INVALID_REQUEST);
				return;
			}
		} else if (this.assembler.revision() != payload.revision()
			|| this.assembler.slotCount() != payload.slotCount()
			|| this.assembler.chunkCount() != payload.chunkCount()) {
			fail(ErrorCode.INVALID_REQUEST);
			return;
		}

		InventorySnapshotAssembler.Status status = this.assembler.accept(
			payload.chunkIndex(), payload.firstSlot(), payload.slots());

		switch (status) {
			case COMPLETE -> {
				List<ItemStack> complete = this.assembler.slots();
				if (complete == null) {
					fail(ErrorCode.INVALID_REQUEST);
					return;
				}
				this.slots = complete;
				this.revision = this.assembler.revision();
				this.state = State.READY;
				this.error = null;
				// A delivered snapshot proves the target is reachable, so the retry budget
				// counts consecutive failures rather than every failure of the session.
				this.attempts = 0;
			}
			case ACCEPTED -> {
				// Still waiting for the remaining chunks.
			}
			default -> fail(ErrorCode.INVALID_REQUEST);
		}
	}

	/**
	 * Applies a server error that belongs to the outstanding request.
	 */
	public void onError(String responseRequestId, ErrorCode code) {
		if (this.state != State.LOADING || this.requestId == null) {
			return;
		}
		if (!this.requestId.equals(responseRequestId)) {
			return;
		}
		fail(code == null ? ErrorCode.INTERNAL_ERROR : code);
	}

	/**
	 * Expires a request that never completed, so the view cannot stay on "Loading"
	 * forever after a dropped response.
	 */
	public void tick() {
		if (this.state == State.LOADING && now() > this.deadlineMillis) {
			fail(ErrorCode.TIMEOUT);
		}
	}

	public void clear() {
		this.target = null;
		this.requestId = null;
		this.state = State.IDLE;
		this.error = null;
		this.slots = List.of();
		this.revision = 0;
		this.deadlineMillis = 0L;
		this.attempts = 0;
		this.retryAfterMillis = 0L;
		this.refreshDueMillis = 0L;
		this.assembler.clear();
	}

	private void fail(ErrorCode code) {
		this.state = State.ERROR;
		this.error = code;
		// The last complete snapshot is retained: a failed background refresh must not make
		// the inventory disappear. Only the partially assembled snapshot is discarded.
		this.retryAfterMillis = now() + this.retryDelayMillis;
		this.assembler.clear();
	}
}
