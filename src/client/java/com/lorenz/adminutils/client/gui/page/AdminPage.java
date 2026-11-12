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

	public abstract void render(RenderContext context, Font font, int x, int y, int width, int height, int mouseX, int mouseY);
}
