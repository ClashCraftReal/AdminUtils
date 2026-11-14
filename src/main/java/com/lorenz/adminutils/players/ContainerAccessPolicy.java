package com.lorenz.adminutils.players;

/**
 * Authorization and slot rules for player container reads.
 *
 * <p>Deliberately free of Minecraft types: the caller resolves the actor from the
 * connection, resolves the target from the server player list, and passes the resulting
 * booleans in. That keeps the security-critical decision directly testable.
 */
public final class ContainerAccessPolicy {
	public enum Decision {
		ALLOW,
		DENY_PERMISSION,
		TARGET_UNAVAILABLE,
		INVALID_SLOT,
		INVALID_CONTAINER
	}

	private ContainerAccessPolicy() {
	}

	/**
	 * A read requires the view permission and an online target. The target UUID is only
	 * a selector; it never substitutes for the permission check.
	 */
	public static Decision decideRead(boolean permissionGranted, boolean targetOnline) {
		if (!permissionGranted) {
			return Decision.DENY_PERMISSION;
		}
		if (!targetOnline) {
			return Decision.TARGET_UNAVAILABLE;
		}
		return Decision.ALLOW;
	}

	/**
	 * Slot bounds are checked against the container's real size, not a constant, so a
	 * version that changes the layout cannot silently read out of range.
	 */
	public static boolean isSlotInRange(int slot, int containerSize) {
		return slot >= 0 && slot < containerSize;
	}

	/**
	 * A snapshot is only sendable when the container reports exactly the size the wire
	 * contract expects.
	 */
	public static boolean hasExpectedSize(int containerSize, PlayerContainerKind kind) {
		return kind != null && containerSize == kind.slotCount();
	}
}
