package com.lorenz.adminutils.client.gui;

//? if <26.1 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Thin drawing facade so pages never reference a version-specific graphics type.
 *
 * <p>Version-specific workaround: 1.21.11 draws through {@code GuiGraphics} with
 * {@code drawString(...)}, while 26.1+ replaced the immediate-mode surface with
 * {@code GuiGraphicsExtractor} and {@code text(...)}. Only this class and
 * {@link AdminScreen} carry that split.
 */
public final class RenderContext {
	//? if <26.1 {
	/*private final GuiGraphics graphics;

	public RenderContext(GuiGraphics graphics) {
		this.graphics = graphics;
	}
	*///?} else {
	private final GuiGraphicsExtractor graphics;

	public RenderContext(GuiGraphicsExtractor graphics) {
		this.graphics = graphics;
	}
	//?}

	public void text(Font font, Component value, int x, int y, int color) {
		//? if <26.1 {
		/*graphics.drawString(font, value, x, y, color, false);
		*///?} else {
		graphics.text(font, value, x, y, color);
		//?}
	}

	public void text(Font font, String value, int x, int y, int color) {
		//? if <26.1 {
		/*graphics.drawString(font, value, x, y, color, false);
		*///?} else {
		graphics.text(font, value, x, y, color);
		//?}
	}

	public void fill(int x1, int y1, int x2, int y2, int color) {
		graphics.fill(x1, y1, x2, y2, color);
	}

	public void outline(int x, int y, int width, int height, int color) {
		fill(x, y, x + width, y + 1, color);
		fill(x, y + height - 1, x + width, y + height, color);
		fill(x, y, x + 1, y + height, color);
		fill(x + width - 1, y, x + width, y + height, color);
	}
}
