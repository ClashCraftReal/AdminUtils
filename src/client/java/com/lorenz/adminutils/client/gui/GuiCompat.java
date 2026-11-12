package com.lorenz.adminutils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Screen switching helper. {@code setScreenAndShow} exists on all four targets, while
 * {@code setScreen} was removed in 26.2, so the shared call site uses the former.
 */
public final class GuiCompat {
	private GuiCompat() {
	}

	public static void openScreen(Screen screen) {
		Minecraft.getInstance().setScreenAndShow(screen);
	}
}
