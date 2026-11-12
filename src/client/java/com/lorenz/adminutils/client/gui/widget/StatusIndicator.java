package com.lorenz.adminutils.client.gui.widget;

import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.RenderContext;
import com.lorenz.adminutils.client.net.ClientSession;
import com.lorenz.adminutils.client.net.ConnectionState;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Compact connection state readout: a coloured dot plus the current state and an
 * optional detail line.
 */
public final class StatusIndicator {
	private StatusIndicator() {
	}

	public static void render(RenderContext context, Font font, int x, int y) {
		ConnectionState state = ClientSession.state();
		int color = colorFor(state);

		context.fill(x, y + 2, x + 6, y + 8, color);
		context.text(font, Component.literal(state.label()), x + 10, y, color);

		String detail = ClientSession.detail();
		if (!detail.isEmpty()) {
			context.text(font, Component.literal(detail), x + 10, y + 10, GuiTheme.TEXT_MUTED);
		}
	}

	public static int colorFor(ConnectionState state) {
		return switch (state) {
			case READY -> GuiTheme.SUCCESS;
			case HANDSHAKING -> GuiTheme.WARNING;
			case INCOMPATIBLE, ERROR -> GuiTheme.ERROR;
			case DISCONNECTED -> GuiTheme.DISABLED;
		};
	}
}
