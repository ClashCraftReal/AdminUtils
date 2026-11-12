package com.lorenz.adminutils.client.gui.widget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.RenderContext;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Transient success/error/info toasts drawn in the top-right corner. Entries expire on
 * their own so the menu never accumulates stale feedback.
 */
public final class NotificationStack {
	private static final long LIFETIME_MILLIS = 5000L;
	private static final int LINE_HEIGHT = 12;

	private final List<Entry> entries = new ArrayList<>();

	public void success(String message) {
		push(Kind.SUCCESS, message);
	}

	public void error(String message) {
		push(Kind.ERROR, message);
	}

	public void info(String message) {
		push(Kind.INFO, message);
	}

	public void clear() {
		entries.clear();
	}

	public int size() {
		return entries.size();
	}

	private void push(Kind kind, String message) {
		entries.add(0, new Entry(kind, message, System.currentTimeMillis()));
		while (entries.size() > 5) {
			entries.remove(entries.size() - 1);
		}
	}

	public void render(RenderContext context, Font font, int right, int top, int maxWidth) {
		long now = System.currentTimeMillis();
		Iterator<Entry> iterator = entries.iterator();
		while (iterator.hasNext()) {
			if (now - iterator.next().createdAtMillis() > LIFETIME_MILLIS) {
				iterator.remove();
			}
		}

		int y = top;
		for (Entry entry : entries) {
			int color = switch (entry.kind()) {
				case SUCCESS -> GuiTheme.SUCCESS;
				case ERROR -> GuiTheme.ERROR;
				case INFO -> GuiTheme.TEXT_MUTED;
			};

			int textWidth = font.width(entry.message());
			int boxWidth = Math.min(maxWidth, textWidth + 12);
			int x = right - boxWidth;

			context.fill(x, y, right, y + LINE_HEIGHT, GuiTheme.PANEL);
			context.outline(x, y, boxWidth, LINE_HEIGHT, color);
			context.text(font, Component.literal(entry.message()), x + 6, y + 2, color);
			y += LINE_HEIGHT + 3;
		}
	}

	private enum Kind {
		SUCCESS,
		ERROR,
		INFO
	}

	private record Entry(Kind kind, String message, long createdAtMillis) {
	}
}
