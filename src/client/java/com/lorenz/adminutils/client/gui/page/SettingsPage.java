package com.lorenz.adminutils.client.gui.page;

import com.lorenz.adminutils.client.config.ClientPreferences;
import com.lorenz.adminutils.client.config.ClientPreferencesIO;
import com.lorenz.adminutils.client.gui.AdminScreen;
import com.lorenz.adminutils.client.gui.AdminUtilsMenuScreen;
import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.NavigationSection;
import com.lorenz.adminutils.client.gui.RenderContext;
import com.lorenz.adminutils.client.gui.widget.AdminButton;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Client-only UI preferences. Changes are written immediately and the screen is
 * rebuilt so toggle labels stay in sync.
 */
public final class SettingsPage extends AdminPage {
	public SettingsPage() {
		super(NavigationSection.SETTINGS);
	}

	@Override
	public Component title() {
		return Component.literal("Settings");
	}

	@Override
	public Component subtitle() {
		return Component.literal("Client-side interface preferences");
	}

	@Override
	public void init(AdminScreen screen, int x, int y, int width, int height) {
		ClientPreferences preferences = ClientPreferencesIO.get();

		screen.addButton(AdminButton.toggle(Component.literal("Permission details"),
			preferences.showPermissionDetails, x, y + 34, 180, 20, pressed -> {
				preferences.showPermissionDetails = !preferences.showPermissionDetails;
				ClientPreferencesIO.save();
				AdminUtilsMenuScreen.open(NavigationSection.SETTINGS);
			}));

		screen.addButton(AdminButton.toggle(Component.literal("Compact layout"),
			preferences.compactLayout, x, y + 58, 180, 20, pressed -> {
				preferences.compactLayout = !preferences.compactLayout;
				ClientPreferencesIO.save();
				AdminUtilsMenuScreen.open(NavigationSection.SETTINGS);
			}));
	}

	@Override
	public void render(RenderContext context, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
		context.text(font, Component.literal("Preferences file"), x, y, GuiTheme.TEXT_MUTED);
		context.text(font, Component.literal(ClientPreferencesIO.file().toString()), x, y + 11, GuiTheme.TEXT);
		context.text(font, Component.literal("These settings never leave this client."), x, y + 30, GuiTheme.TEXT_MUTED);
	}
}
