package com.lorenz.adminutils.players;

/**
 * Which server-side container a player-data request addresses.
 *
 * <p>The slot counts are the authoritative wire bounds: {@code Inventory#getContainerSize()}
 * is 36 storage slots plus the 7 equipment slots (feet, legs, chest, head, offhand, body,
 * saddle), and {@code PlayerEnderChestContainer} is a 27-slot {@code SimpleContainer}.
 * Both values were verified against the 1.21.11 and 26.x Minecraft artifacts.
 */
public enum PlayerContainerKind {
	INVENTORY(43),
	ENDER_CHEST(27);

	private final int slotCount;

	PlayerContainerKind(int slotCount) {
		this.slotCount = slotCount;
	}

	public int slotCount() {
		return slotCount;
	}
}
