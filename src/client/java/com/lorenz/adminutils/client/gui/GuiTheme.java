package com.lorenz.adminutils.client.gui;

/**
 * Shared colours and metrics. Values are ARGB so they can be passed straight to
 * {@link RenderContext#fill(int, int, int, int, int)}.
 */
public final class GuiTheme {
	public static final int BACKDROP = 0xF0101014;
	public static final int NAV_BACKGROUND = 0xFF181820;
	public static final int PANEL = 0xFF1E1E26;
	public static final int BORDER = 0xFF2E2E3A;
	public static final int TEXT = 0xFFE8E8EC;
	public static final int TEXT_MUTED = 0xFF9A9AA6;
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

	private GuiTheme() {
	}
}
