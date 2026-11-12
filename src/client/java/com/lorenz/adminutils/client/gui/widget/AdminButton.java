package com.lorenz.adminutils.client.gui.widget;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Factory for the standard AdminUtils button. Disabled state is expressed through
 * {@code active} so vanilla rendering and narration stay consistent.
 */
public final class AdminButton {
	private AdminButton() {
	}

	public static Button create(Component label, int x, int y, int width, int height, boolean enabled, Button.OnPress onPress) {
		Button button = Button.builder(label, onPress).bounds(x, y, width, height).build();
		button.active = enabled;
		return button;
	}

	public static Button toggle(Component label, boolean state, int x, int y, int width, int height, Button.OnPress onPress) {
		return create(Component.literal(label.getString() + ": " + (state ? "ON" : "OFF")), x, y, width, height, true, onPress);
	}
}
