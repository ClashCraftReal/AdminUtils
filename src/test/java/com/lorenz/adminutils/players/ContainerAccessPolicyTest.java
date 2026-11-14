package com.lorenz.adminutils.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContainerAccessPolicyTest {
	@Test
	void allowsWhenPermissionGrantedAndTargetOnline() {
		assertEquals(ContainerAccessPolicy.Decision.ALLOW, ContainerAccessPolicy.decideRead(true, true));
	}

	@Test
	void deniesWithoutPermissionEvenWhenTheTargetIsOnline() {
		assertEquals(ContainerAccessPolicy.Decision.DENY_PERMISSION, ContainerAccessPolicy.decideRead(false, true));
	}

	@Test
	void permissionIsCheckedBeforeTargetAvailability() {
		// An unauthorized caller must not learn whether a UUID is online.
		assertEquals(ContainerAccessPolicy.Decision.DENY_PERMISSION, ContainerAccessPolicy.decideRead(false, false));
	}

	@Test
	void rejectsAnOfflineTargetWhenPermitted() {
		assertEquals(ContainerAccessPolicy.Decision.TARGET_UNAVAILABLE, ContainerAccessPolicy.decideRead(true, false));
	}

	@Test
	void slotBoundsFollowTheContainerSize() {
		assertTrue(ContainerAccessPolicy.isSlotInRange(0, 43));
		assertTrue(ContainerAccessPolicy.isSlotInRange(42, 43));
		assertFalse(ContainerAccessPolicy.isSlotInRange(43, 43));
		assertFalse(ContainerAccessPolicy.isSlotInRange(-1, 43));
	}

	@Test
	void expectedSizeMatchesTheWireContract() {
		assertTrue(ContainerAccessPolicy.hasExpectedSize(43, PlayerContainerKind.INVENTORY));
		assertFalse(ContainerAccessPolicy.hasExpectedSize(42, PlayerContainerKind.INVENTORY));
		assertTrue(ContainerAccessPolicy.hasExpectedSize(27, PlayerContainerKind.ENDER_CHEST));
		assertFalse(ContainerAccessPolicy.hasExpectedSize(43, PlayerContainerKind.ENDER_CHEST));
		assertFalse(ContainerAccessPolicy.hasExpectedSize(43, null));
	}
}
