package com.lorenz.adminutils.client.gui.page.players;

import java.util.List;

import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.RenderContext;
import com.lorenz.adminutils.client.net.PlayerInventoryStore;
import com.lorenz.adminutils.net.ErrorCode;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Read-only inventory grid for the selected player.
 *
 * <p>Slots are drawn in the vanilla layout: the three main rows (slots 9-35) on top and the
 * hotbar (slots 0-8) on the bottom row. Only the visual position changes; the slot indices
 * are the ones the server sent. Armor and offhand are drawn on their own labelled row, and
 * the two additional equipment slots are only shown when they actually hold something, so
 * they are never mislabelled as ordinary inventory.
 *
 * <p>Nothing here can modify a stack: the panel only draws what the server sent.
 */
public final class InventoryPanel {
	private static final int COLUMNS = 9;
	private static final int MAIN_ROWS = 3;
	/** Visual grid row that holds the hotbar; rows 0-2 hold main inventory slots 9-35. */
	public static final int HOTBAR_ROW = MAIN_ROWS;
	private static final int HOTBAR_GAP = 2;
	private static final int MIN_CELL = 12;
	private static final int MAX_CELL = 18;
	private static final int LABEL_STEP = 10;
	private static final int SECTION_GAP = 4;

	private InventoryPanel() {
	}

	public static int preferredHeight(int width) {
		int cell = cellSize(width);
		return LABEL_STEP + (4 * cell) + HOTBAR_GAP + SECTION_GAP + LABEL_STEP + cell;
	}

	/**
	 * Minecraft slot index drawn at the given visual grid position, or {@code -1} when the
	 * position is outside the grid. Rows 0-2 are the main inventory (9-35) and row 3 is the
	 * hotbar (0-8), matching the vanilla inventory screen.
	 */
	public static int slotAt(int gridRow, int column) {
		if (column < 0 || column >= COLUMNS || gridRow < 0 || gridRow > HOTBAR_ROW) {
			return -1;
		}
		if (gridRow == HOTBAR_ROW) {
			return column;
		}
		return COLUMNS + (gridRow * COLUMNS) + column;
	}

	/** Vertical offset of a grid row, with the vanilla gap above the hotbar. */
	public static int rowOffset(int gridRow, int cell) {
		return gridRow * cell + (gridRow >= HOTBAR_ROW ? HOTBAR_GAP : 0);
	}

	public static void render(RenderContext context, Font font, PlayerInventoryStore store,
		int x, int y, int width, int height) {
		context.text(font, Component.literal("Inventory"), x, y, GuiTheme.TEXT);
		context.text(font, Component.literal("read-only"), x + 52, y, GuiTheme.DISABLED);

		int contentY = y + LABEL_STEP;

		if (store == null || store.state() == PlayerInventoryStore.State.IDLE) {
			// The panel is only drawn for a selected player, so IDLE means the request has
			// not been sent yet rather than "nothing to show".
			context.text(font, Component.literal("Requesting inventory..."), x, contentY, GuiTheme.TEXT_MUTED);
			return;
		}

		List<ItemStack> slots = store.slots();
		if (slots.size() < 41) {
			// No complete snapshot yet: this is the first load, so a blocking state is right.
			if (store.state() == PlayerInventoryStore.State.LOADING) {
				context.text(font, Component.literal("Loading inventory..."), x, contentY, GuiTheme.TEXT_MUTED);
				return;
			}
			if (store.state() == PlayerInventoryStore.State.ERROR) {
				context.text(font, Component.literal("Inventory not loaded: " + messageFor(store.error())), x, contentY,
					GuiTheme.ERROR);
				int attempts = store.attempts();
				String retry = attempts < PlayerInventoryStore.maxAttempts()
					? "Retrying automatically (attempt " + (attempts + 1) + " of " + PlayerInventoryStore.maxAttempts() + ")."
					: "Retry limit reached. Reopen the page to try again.";
				context.text(font, Component.literal(retry), x, contentY + LABEL_STEP, GuiTheme.TEXT_MUTED);
				return;
			}
			context.text(font, Component.literal("Inventory data is incomplete."), x, contentY, GuiTheme.ERROR);
			return;
		}

		// A complete snapshot is available, so it stays on screen while a refresh runs and
		// after a failed refresh. Only the status line changes.
		String status = statusFor(store);
		if (!status.isEmpty()) {
			context.text(font, Component.literal(status), x + width - font.width(status), y,
				store.state() == PlayerInventoryStore.State.ERROR ? GuiTheme.ERROR : GuiTheme.TEXT_MUTED);
		}

		int cell = cellSize(width);
		for (int row = 0; row <= HOTBAR_ROW; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				int slot = slotAt(row, column);
				drawSlot(context, font, slots.get(slot), x + column * cell,
					contentY + rowOffset(row, cell), cell);
			}
		}

		int armorY = contentY + rowOffset(HOTBAR_ROW, cell) + cell + SECTION_GAP;
		context.text(font, Component.literal("Armor"), x, armorY, GuiTheme.TEXT_MUTED);
		int armorGridY = armorY + LABEL_STEP;
		for (int offset = 0; offset < 4; offset++) {
			drawSlot(context, font, slots.get(36 + offset), x + offset * cell, armorGridY, cell);
		}
		drawSlot(context, font, slots.get(40), x + 5 * cell, armorGridY, cell);
		context.text(font, Component.literal("Offhand"), x + 6 * cell + 2, armorGridY + (cell - font.lineHeight) / 2,
			GuiTheme.DISABLED);

		if (slots.size() > 42 && (!slots.get(41).isEmpty() || !slots.get(42).isEmpty())) {
			int extraY = armorGridY + cell + SECTION_GAP;
			context.text(font, Component.literal("Additional equipment"), x, extraY, GuiTheme.TEXT_MUTED);
			int extraGridY = extraY + LABEL_STEP;
			drawSlot(context, font, slots.get(41), x, extraGridY, cell);
			drawSlot(context, font, slots.get(42), x + cell, extraGridY, cell);
		}
	}

	/**
	 * Short, non-blocking warning shown next to the header while a snapshot is displayed.
	 * Empty while the displayed snapshot is current, including during a background
	 * refresh, so a routine refresh never draws attention.
	 */
	private static String statusFor(PlayerInventoryStore store) {
		return store.state() == PlayerInventoryStore.State.ERROR
			? "Refresh failed: " + messageFor(store.error())
			: "";
	}

	private static void drawSlot(RenderContext context, Font font, ItemStack stack, int x, int y, int cell) {
		context.fill(x, y, x + cell, y + cell, GuiTheme.NAV_BACKGROUND);
		context.outline(x, y, cell, cell, GuiTheme.BORDER);
		if (stack == null || stack.isEmpty()) {
			return;
		}
		context.item(stack, x + 1, y + 1);
		context.itemDecorations(font, stack, x + 1, y + 1);
	}

	private static int cellSize(int width) {
		int available = Math.max(0, width - 2);
		int cell = available / COLUMNS;
		return Math.max(MIN_CELL, Math.min(MAX_CELL, cell));
	}

	private static String messageFor(ErrorCode code) {
		if (code == null) {
			return "Inventory unavailable.";
		}
		return switch (code) {
			case PERMISSION_DENIED -> "Permission denied: players.inventory.view";
			case TARGET_UNAVAILABLE -> "That player is no longer online.";
			case RATE_LIMITED -> "Too many requests. Try again shortly.";
			case SNAPSHOT_TOO_LARGE -> "Inventory is too large to transfer.";
			case TIMEOUT -> "The server did not answer in time.";
			case INVALID_REQUEST, INVALID_SLOT -> "The server rejected the request.";
			default -> "Inventory unavailable.";
		};
	}
}
