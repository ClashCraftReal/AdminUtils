package com.lorenz.adminutils.client.gui;

import com.lorenz.adminutils.client.gui.page.AdminPage;
import com.lorenz.adminutils.client.gui.page.HomePage;
import com.lorenz.adminutils.client.gui.page.ItemsPage;
import com.lorenz.adminutils.client.gui.page.PermissionsPage;
import com.lorenz.adminutils.client.gui.page.PlayersPage;
import com.lorenz.adminutils.client.gui.page.SettingsPage;
import com.lorenz.adminutils.client.gui.page.ToolsPage;
import com.lorenz.adminutils.client.net.ClientNetworkHandler;

/**
 * The single AdminUtils screen. Navigation swaps the active {@link AdminPage} and
 * rebuilds the screen, so every section shares the same chrome and widget lifecycle.
 */
public final class AdminUtilsMenuScreen extends AdminScreen {
	private AdminUtilsMenuScreen(NavigationSection section) {
		super(section);
	}

	public static void open(NavigationSection section) {
		AdminScreen.beginNavigation();
		GuiCompat.openScreen(new AdminUtilsMenuScreen(section == null ? NavigationSection.HOME : section));
		ClientNetworkHandler.requestPermissions();
	}

	@Override
	protected AdminPage createPage() {
		return switch (section()) {
			case HOME -> new HomePage();
			case ITEMS -> new ItemsPage();
			case PLAYERS -> new PlayersPage();
			case TOOLS -> new ToolsPage();
			case PERMISSIONS -> new PermissionsPage();
			case SETTINGS -> new SettingsPage();
		};
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
