package com.lorenz.adminutils.client.gui.widget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.lorenz.adminutils.client.gui.GuiAnimation;
import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.RenderContext;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Transient success/error/info toasts drawn in the top-right corner. Entries expire on
 * their own so the menu never accumulates stale feedback.
 *
 * <p>The menu uses {@link #shared()} so feedback survives a section change: navigating
 * replaces the screen object, and a per-screen stack would drop every pending toast.
 */
public final class NotificationStack {
	private static final long LIFETIME_MILLIS = 5000L;
	private static final int LINE_HEIGHT = 12;
	private static final int TEXT_INSET = 6;
	private static final String ELLIPSIS = "...";

	private static final NotificationStack SHARED = new NotificationStack();

	private final List<Entry> entries = new ArrayList<>();

	public static NotificationStack shared() {
		return SHARED;
	}

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
				case INFO -> GuiTheme.accent();
			};
			float reveal = GuiAnimation.progress(entry.createdAtMillis(), GuiAnimation.FAST_MILLIS);

			int textWidth = font.width(entry.message());
			int boxWidth = Math.min(maxWidth, textWidth + TEXT_INSET * 2);
			int x = right - boxWidth;

			context.fill(x, y, right, y + LINE_HEIGHT, GuiAnimation.withAlpha(GuiTheme.PANEL, reveal));
			context.outline(x, y, boxWidth, LINE_HEIGHT, GuiAnimation.withAlpha(color, reveal));
			context.text(font, Component.literal(fit(font, entry.message(), boxWidth - TEXT_INSET * 2)),
				x + TEXT_INSET, y + 2, GuiAnimation.withAlpha(color, reveal));
			y += LINE_HEIGHT + 3;
		}
	}

	/**
	 * Shortens a message so it cannot draw past the toast border. The box is capped at
	 * {@code maxWidth}, so an untruncated string would otherwise overflow it.
	 */
	private static String fit(Font font, String message, int limit) {
		if (limit <= 0) {
			return "";
		}
		String fitted = font.plainSubstrByWidth(message, limit);
		if (fitted.length() == message.length()) {
			return fitted;
		}
		int ellipsisWidth = font.width(ELLIPSIS);
		if (limit <= ellipsisWidth) {
			return fitted;
		}
		return font.plainSubstrByWidth(message, limit - ellipsisWidth) + ELLIPSIS;
	}

	private enum Kind {
		SUCCESS,
		ERROR,
		INFO
	}

	private record Entry(Kind kind, String message, long createdAtMillis) {
	}
}
