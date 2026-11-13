package com.lorenz.adminutils.client.gui.page;

import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.NavigationSection;
import com.lorenz.adminutils.client.gui.RenderContext;
import com.lorenz.adminutils.client.net.ClientSession;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Landing page: connection state, identity of the current session and a short
 * permission summary.
 */
public final class HomePage extends AdminPage {
	public HomePage() {
		super(NavigationSection.HOME);
	}

	@Override
	public Component title() {
		return Component.literal("Home");
	}

	@Override
	public Component subtitle() {
		return Component.literal("Administration overview");
	}

	@Override
	public void render(RenderContext context, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
		int line = y;
		line = row(context, font, x, line, "Connection", ClientSession.state().label(), GuiTheme.TEXT);
		line = row(context, font, x, line, "Protocol", "v" + com.lorenz.adminutils.net.Protocol.VERSION, GuiTheme.TEXT);
		line = row(context, font, x, line, "Operator", ClientSession.snapshot().operator() ? "yes" : "no", GuiTheme.TEXT);
		line = row(context, font, x, line, "Permissions", ClientSession.snapshot().granted().isEmpty()
			? "none" : String.join(", ", ClientSession.snapshot().granted()), GuiTheme.TEXT);

		line += GuiTheme.sectionGap();
		context.text(font, Component.literal("Sections"), x, line, GuiTheme.TEXT);
		line += GuiTheme.rowHeight() + 1;

		for (NavigationSection section : NavigationSection.values()) {
			boolean permitted = section.isAccessible(ClientSession.can(section.permission()));
			String state = section.isPlaceholder() ? "placeholder" : permitted ? "available" : "no access";
			int color = permitted && !section.isPlaceholder() ? GuiTheme.SUCCESS : GuiTheme.TEXT_MUTED;
			line = row(context, font, x, line, section.label(), state, color);
		}

		line += GuiTheme.sectionGap();
		context.text(font, Component.literal("Item tools are not implemented in this phase."), x, line, GuiTheme.TEXT_MUTED);
	}

	private static int row(RenderContext context, Font font, int x, int y, String label, String value, int valueColor) {
		context.text(font, Component.literal(label), x, y, GuiTheme.TEXT_MUTED);
		context.text(font, Component.literal(value), x + 86, y, valueColor);
		return y + GuiTheme.rowHeight();
	}
}
