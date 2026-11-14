package com.lorenz.adminutils.client.gui.page.players;

import java.util.UUID;

import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.RenderContext;
import com.lorenz.adminutils.client.net.PlayerInventoryStore;
import com.lorenz.adminutils.client.player.PlayerListEntry;
import com.lorenz.adminutils.client.player.SkinTextureCache;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Right-hand profile for the selected player: head, name, UUID, online state and the
 * read-only inventory view.
 *
 * <p>Ender Chest access belongs to a later stage, so this panel says so explicitly
 * instead of drawing placeholder slots that would look functional.
 */
public final class PlayerProfilePanel {
	private static final int HEAD_SIZE = 32;
	private static final int HEAD_GAP = 8;
	private static final int LINE_STEP = 11;
	private static final int LABEL_WIDTH = 46;

	private PlayerProfilePanel() {
	}

	public static void render(RenderContext context, Font font, PlayerListEntry entry, UUID localPlayer,
		int x, int y, int width, int height, SkinTextureCache skins, PlayerInventoryStore inventory) {
		context.fill(x, y, x + width, y + height, GuiTheme.PANEL);
		context.outline(x, y, width, height, GuiTheme.BORDER);

		if (entry == null) {
			context.text(font, Component.literal("Select a player"), x + 8, y + 8, GuiTheme.TEXT_MUTED);
			context.text(font, Component.literal("No online player is selected."), x + 8, y + 8 + LINE_STEP,
				GuiTheme.DISABLED);
			return;
		}

		PlayerHeadRenderer.render(context, font, skins.textureFor(entry.id()), entry.name(), x + 8, y + 8, HEAD_SIZE);

		int textX = x + 8 + HEAD_SIZE + HEAD_GAP;
		context.text(font, Component.literal(entry.name()), textX, y + 10, GuiTheme.TEXT);
		context.text(font, Component.literal(entry.id().equals(localPlayer) ? "Online - this is you" : "Online"),
			textX, y + 10 + LINE_STEP, GuiTheme.SUCCESS);

		int line = y + 8 + HEAD_SIZE + HEAD_GAP;
		line = row(context, font, x + 8, line, "UUID", entry.id().toString());
		line = row(context, font, x + 8, line, "Status", "Online");

		line += GuiTheme.sectionGap();
		InventoryPanel.render(context, font, inventory, x + 8, line, width - 16,
			Math.max(0, y + height - line - 8));

		int footer = y + height - LINE_STEP - 2;
		if (footer > line) {
			context.text(font, Component.literal("Ender Chest view arrives in a later stage."),
				x + 8, footer, GuiTheme.DISABLED);
		}
	}

	private static int row(RenderContext context, Font font, int x, int y, String label, String value) {
		context.text(font, Component.literal(label), x, y, GuiTheme.TEXT_MUTED);
		context.text(font, Component.literal(value), x + LABEL_WIDTH, y, GuiTheme.TEXT);
		return y + LINE_STEP;
	}
}
