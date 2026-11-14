package com.lorenz.adminutils.client.gui.page.players;

import java.util.List;
import java.util.UUID;

import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.RenderContext;
import com.lorenz.adminutils.client.player.PlayerListEntry;
import com.lorenz.adminutils.client.player.SkinTextureCache;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Scrollable online player list. Drawn rather than built from widgets so a join or
 * leave only mutates a list instead of rebuilding the screen.
 */
public final class PlayerListPanel {
	public static final int ROW_HEIGHT = 22;
	public static final int HEAD_SIZE = 16;
	private static final int HEAD_INSET = 3;
	private static final int NAME_OFFSET = 24;
	private static final int SCROLL_STEP = 18;

	private PlayerListPanel() {
	}

	public static int visibleRows(int height) {
		return Math.max(1, height / ROW_HEIGHT);
	}

	public static int maxScroll(int entryCount, int height) {
		int contentHeight = entryCount * ROW_HEIGHT;
		return Math.max(0, contentHeight - Math.max(0, height));
	}

	public static int clampScroll(int scroll, int entryCount, int height) {
		return Math.max(0, Math.min(scroll, maxScroll(entryCount, height)));
	}

	public static int scrollBy(int scroll, double vertical, int entryCount, int height) {
		return clampScroll(scroll - (int) Math.round(vertical * SCROLL_STEP), entryCount, height);
	}

	/**
	 * Index of the row under the cursor, or -1 when the cursor is outside the list.
	 */
	public static int rowAt(double mouseX, double mouseY, int x, int y, int width, int height,
		int scroll, int entryCount) {
		if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
			return -1;
		}
		int index = (int) ((mouseY - y + scroll) / ROW_HEIGHT);
		return index >= 0 && index < entryCount ? index : -1;
	}

	public static void render(RenderContext context, Font font, List<PlayerListEntry> entries, UUID selected,
		UUID localPlayer, int x, int y, int width, int height, int scroll, int mouseX, int mouseY,
		SkinTextureCache skins) {
		context.fill(x, y, x + width, y + height, GuiTheme.PANEL);
		context.outline(x, y, width, height, GuiTheme.BORDER);

		if (entries.isEmpty()) {
			context.text(font, Component.literal("No players match"), x + 6, y + 6, GuiTheme.TEXT_MUTED);
			return;
		}

		int firstRow = Math.max(0, scroll / ROW_HEIGHT);
		int lastRow = Math.min(entries.size(), firstRow + visibleRows(height) + 1);
		int hovered = rowAt(mouseX, mouseY, x, y, width, height, scroll, entries.size());

		for (int index = firstRow; index < lastRow; index++) {
			PlayerListEntry entry = entries.get(index);
			int rowY = y + index * ROW_HEIGHT - scroll;
			if (rowY + ROW_HEIGHT < y || rowY > y + height) {
				continue;
			}

			boolean isSelected = entry.id().equals(selected);
			boolean isHovered = index == hovered;
			if (isSelected) {
				context.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, GuiTheme.BORDER);
				context.fill(x + 1, rowY, x + 3, rowY + ROW_HEIGHT, GuiTheme.accent());
			} else if (isHovered) {
				context.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, GuiTheme.NAV_BACKGROUND);
			}

			PlayerHeadRenderer.render(context, font, skins.textureFor(entry.id()), entry.name(),
				x + HEAD_INSET + 2, rowY + HEAD_INSET, HEAD_SIZE);

			String label = entry.id().equals(localPlayer) ? entry.name() + " (you)" : entry.name();
			int nameColor = isSelected ? GuiTheme.TEXT : GuiTheme.TEXT_MUTED;
			context.text(font, Component.literal(label), x + NAME_OFFSET, rowY + (ROW_HEIGHT - font.lineHeight) / 2,
				nameColor);		}
	}
}
