package com.lorenz.adminutils.client.gui;

//? if <26.1 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

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

	/**
	 * Draws a texture region. The four integers are corner coordinates
	 * ({@code x0, y0, x1, y1}), not a position and a size, and the four floats are
	 * normalised UVs. Both graphics generations forward to the same
	 * {@code innerBlit(pipeline, texture, x0, y0, x1, y1, u0, u1, v0, v1, color)}
	 * shape, so only the receiver type differs.
	 */
	public void blit(Identifier texture, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {
		//? if <26.1 {
		/*graphics.blit(texture, x0, y0, x1, y1, u0, u1, v0, v1);
		*///?} else {
		graphics.blit(texture, x0, y0, x1, y1, u0, u1, v0, v1);
		//?}
	}

	/**
	 * Draws an item icon. 1.21.11 names the operation {@code renderItem} and 26.1+
	 * renamed it to {@code item}; both take the same stack and top-left corner.
	 */
	public void item(ItemStack stack, int x, int y) {
		//? if <26.1 {
		/*graphics.renderItem(stack, x, y);
		*///?} else {
		graphics.item(stack, x, y);
		//?}
	}

	/**
	 * Draws the stack count and durability bar over an icon already drawn by
	 * {@link #item}. 1.21.11 calls this {@code renderItemDecorations}.
	 */
	public void itemDecorations(Font font, ItemStack stack, int x, int y) {
		//? if <26.1 {
		/*graphics.renderItemDecorations(font, stack, x, y);
		*///?} else {
		graphics.itemDecorations(font, stack, x, y);
		//?}
	}

	public void outline(int x, int y, int width, int height, int color) {		fill(x, y, x + width, y + 1, color);
		fill(x, y + height - 1, x + width, y + height, color);
		fill(x, y, x + 1, y + height, color);
		fill(x + width - 1, y, x + width, y + height, color);
	}

	/**
	 * Restricts subsequent drawing to the given rectangle. {@code enableScissor} and
	 * {@code disableScissor} exist on both graphics generations, so this needs no
	 * version split.
	 */
	public void scissor(int x, int y, int width, int height) {
		graphics.enableScissor(x, y, x + width, y + height);
	}

	public void resetScissor() {
		graphics.disableScissor();
	}
}
