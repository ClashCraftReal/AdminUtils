package com.lorenz.adminutils.client.gui.page;

import com.lorenz.adminutils.client.gui.AdminScreen;
import com.lorenz.adminutils.client.gui.NavigationSection;
import com.lorenz.adminutils.client.gui.RenderContext;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * One navigable section of the AdminUtils menu. Pages own their widgets and drawing;
 * the surrounding chrome lives in {@link AdminScreen}.
 */
public abstract class AdminPage {
	private final NavigationSection section;

	protected AdminPage(NavigationSection section) {
		this.section = section;
	}

	public NavigationSection section() {
		return section;
	}

	public abstract Component title();

	public abstract Component subtitle();

	public void init(AdminScreen screen, int x, int y, int width, int height) {
	}

	/**
	 * Called once per client tick while the screen is open. Pages use it to refresh
	 * client-side state that changes without a widget rebuild, such as the online
	 * player list.
	 */
	public void tick() {
	}

	/**
	 * Mouse press inside the page area. Widgets are offered the event first by
	 * {@link AdminScreen}, so a page only sees clicks that no widget consumed.
	 *
	 * @return true when the page handled the click
	 */
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	/**
	 * Scroll inside the page area, offered after the widgets decline it.
	 *
	 * @return true when the page handled the scroll
	 */
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		return false;
	}

	public abstract void render(RenderContext context, Font font, int x, int y, int width, int height, int mouseX, int mouseY);
}
