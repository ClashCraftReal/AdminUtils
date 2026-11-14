package com.lorenz.adminutils.client.gui.page.players;

import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.RenderContext;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Draws a front-facing player head from a skin texture.
 *
 * <p>UVs are taken from the vanilla 64x64 skin layout: the face occupies
 * {@code (8,8)-(16,16)} and the hat overlay {@code (40,8)-(48,16)}. The overlay is
 * drawn on top so hair and headwear appear as they do in game.
 *
 * <p>{@code PlayerFaceRenderer} is deliberately not used: it exists in 1.21.11 but is
 * absent from the 26.x targets, so it would force a version split for no benefit.
 */
public final class PlayerHeadRenderer {
	private static final float FACE_U0 = 8.0F / 64.0F;
	private static final float FACE_U1 = 16.0F / 64.0F;
	private static final float FACE_V0 = 8.0F / 64.0F;
	private static final float FACE_V1 = 16.0F / 64.0F;
	private static final float HAT_U0 = 40.0F / 64.0F;
	private static final float HAT_U1 = 48.0F / 64.0F;

	private PlayerHeadRenderer() {
	}

	/**
	 * Draws the head at {@code size} pixels square. When the texture is missing the
	 * slot is still painted, with the player's initial, so the row keeps its shape.
	 */
	public static void render(RenderContext context, Font font, Identifier texture, String name,
		int x, int y, int size) {
		if (texture == null) {
			renderFallback(context, font, name, x, y, size);
			return;
		}
		try {
			context.blit(texture, x, y, x + size, y + size, FACE_U0, FACE_U1, FACE_V0, FACE_V1);
			context.blit(texture, x, y, x + size, y + size, HAT_U0, HAT_U1, FACE_V0, FACE_V1);
		} catch (RuntimeException exception) {
			// A texture that failed to upload must not abort the whole page render.
			renderFallback(context, font, name, x, y, size);
		}
	}

	private static void renderFallback(RenderContext context, Font font, String name, int x, int y, int size) {
		context.fill(x, y, x + size, y + size, GuiTheme.BORDER);
		context.outline(x, y, size, size, GuiTheme.DISABLED);
		String initial = name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
		int textWidth = font.width(initial);
		context.text(font, Component.literal(initial), x + (size - textWidth) / 2,
			y + (size - font.lineHeight) / 2, GuiTheme.TEXT_MUTED);
	}
}
