package com.lorenz.adminutils.client.gui.page;

import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.NavigationSection;
import com.lorenz.adminutils.client.gui.RenderContext;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Shared body for sections that exist in the navigation but have no implementation
 * yet. Rendering an explicit notice is preferable to an empty panel that looks broken.
 */
public abstract class PlaceholderPage extends AdminPage {
	private final String heading;
	private final String note;

	protected PlaceholderPage(NavigationSection section, String heading, String note) {
		super(section);
		this.heading = heading;
		this.note = note;
	}

	@Override
	public Component title() {
		return Component.literal(heading);
	}

	@Override
	public Component subtitle() {
		return Component.literal("Placeholder - not implemented in this phase");
	}

	@Override
	public void render(RenderContext context, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
		context.text(font, Component.literal(note), x, y, GuiTheme.TEXT_MUTED);
		context.text(font, Component.literal("Controls for this section stay disabled until a later phase."),
			x, y + 12, GuiTheme.DISABLED);
	}
}
