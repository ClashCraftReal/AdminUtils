package com.lorenz.adminutils.client.gui.page.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The panel draws the vanilla inventory layout without renumbering anything: the visual
 * grid maps onto the slot indices the server sent.
 */
class InventoryPanelLayoutTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void mainInventoryRowsHoldSlotsNineToThirtyFive() {
		assertEquals(9, InventoryPanel.slotAt(0, 0));
		assertEquals(17, InventoryPanel.slotAt(0, 8));
		assertEquals(18, InventoryPanel.slotAt(1, 0));
		assertEquals(26, InventoryPanel.slotAt(1, 8));
		assertEquals(27, InventoryPanel.slotAt(2, 0));
		assertEquals(35, InventoryPanel.slotAt(2, 8));
	}

	@Test
	void hotbarIsTheBottomRow() {
		assertEquals(3, InventoryPanel.HOTBAR_ROW);
		for (int column = 0; column < 9; column++) {
			assertEquals(column, InventoryPanel.slotAt(InventoryPanel.HOTBAR_ROW, column));
		}
	}

	@Test
	void everyInventorySlotIsDrawnExactlyOnce() {
		Set<Integer> seen = new HashSet<>();
		for (int row = 0; row <= InventoryPanel.HOTBAR_ROW; row++) {
			for (int column = 0; column < 9; column++) {
				int slot = InventoryPanel.slotAt(row, column);
				assertTrue(slot >= 0 && slot < 36, "slot out of range: " + slot);
				assertTrue(seen.add(slot), "duplicate slot: " + slot);
			}
		}
		assertEquals(36, seen.size());
	}

	@Test
	void positionsOutsideTheGridAreRejected() {
		assertEquals(-1, InventoryPanel.slotAt(-1, 0));
		assertEquals(-1, InventoryPanel.slotAt(InventoryPanel.HOTBAR_ROW + 1, 0));
		assertEquals(-1, InventoryPanel.slotAt(0, -1));
		assertEquals(-1, InventoryPanel.slotAt(0, 9));
	}

	@Test
	void hotbarRowSitsBelowTheMainRowsWithAGap() {
		int cell = 18;
		int mainLastRow = InventoryPanel.rowOffset(2, cell);
		int hotbar = InventoryPanel.rowOffset(InventoryPanel.HOTBAR_ROW, cell);

		assertEquals(2 * cell, mainLastRow);
		assertTrue(hotbar > mainLastRow + cell, "hotbar must start below the last main row");
	}
}
