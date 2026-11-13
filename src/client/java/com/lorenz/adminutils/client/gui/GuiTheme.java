package com.lorenz.adminutils.client.gui;

import com.lorenz.adminutils.client.config.AccentColor;
import com.lorenz.adminutils.client.config.ClientPreferencesIO;

/**
 * Shared colours and metrics. Values are ARGB so they can be passed straight to
 * {@link RenderContext#fill(int, int, int, int, int)}.
 *
 * <p>The {@code *_NORMAL} constants describe the default layout. The accessor methods
 * below return the values for the layout currently selected in the client
 * preferences, so call sites never branch on {@code compactLayout} themselves.
 */
public final class GuiTheme {
	public static final int BACKDROP = 0xF0101014;
	public static final int NAV_BACKGROUND = 0xFF181820;
	public static final int PANEL = 0xFF1E1E26;
	public static final int BORDER = 0xFF2E2E3A;
	public static final int TEXT = 0xFFE8E8EC;
	public static final int TEXT_MUTED = 0xFF9A9AA6;
	/** Default accent; the live value comes from {@link #accent()}. */
	public static final int ACCENT = 0xFF3D7BDF;
	public static final int SUCCESS = 0xFF4CAF50;
	public static final int ERROR = 0xFFE05252;
	public static final int WARNING = 0xFFE0A030;
	public static final int DISABLED = 0xFF5A5A66;

	public static final int NAV_WIDTH = 116;
	public static final int PADDING = 10;
	public static final int NAV_ITEM_HEIGHT = 20;
	public static final int NAV_ITEM_GAP = 4;
	public static final int NAV_TOP = 34;
	public static final int HEADER_HEIGHT = 36;
	public static final int ROW_HEIGHT = 11;
	public static final int SECTION_GAP = 8;

	private GuiTheme() {
	}

	/**
	 * Accent colour selected in the client preferences. Unknown stored identifiers have
	 * already been normalised to the default, so this never returns an invalid colour.
	 */
	public static int accent() {
		return AccentColor.byId(ClientPreferencesIO.get().accentColor).argb();
	}

	public static boolean compact() {
		return ClientPreferencesIO.get().compactLayout;
	}

	public static int padding() {
		return compact() ? 8 : PADDING;
	}

	public static int navItemHeight() {
		return compact() ? 16 : NAV_ITEM_HEIGHT;
	}

	public static int navItemGap() {
		return compact() ? 2 : NAV_ITEM_GAP;
	}

	public static int navTop() {
		return compact() ? 30 : NAV_TOP;
	}

	public static int headerHeight() {
		return compact() ? 32 : HEADER_HEIGHT;
	}

	public static int rowHeight() {
		return compact() ? 9 : ROW_HEIGHT;
	}

	public static int sectionGap() {
		return compact() ? 5 : SECTION_GAP;
	}
}
