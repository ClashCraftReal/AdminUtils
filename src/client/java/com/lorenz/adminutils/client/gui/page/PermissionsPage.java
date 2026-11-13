package com.lorenz.adminutils.client.gui.page;

import com.lorenz.adminutils.api.AdminPermission;
import com.lorenz.adminutils.api.PermissionSnapshot;
import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.NavigationSection;
import com.lorenz.adminutils.client.gui.RenderContext;
import com.lorenz.adminutils.client.net.ClientSession;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Read-only view of the snapshot the server sent for this connection. It explains what
 * the UI may enable; it is not an authority and cannot grant anything.
 */
public final class PermissionsPage extends AdminPage {
	public PermissionsPage() {
		super(NavigationSection.PERMISSIONS);
	}

	@Override
	public Component title() {
		return Component.literal("Permissions");
	}

	@Override
	public Component subtitle() {
		return Component.literal("Server-reported permissions for this session");
	}

	@Override
	public void render(RenderContext context, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
		PermissionSnapshot snapshot = ClientSession.snapshot();
		int line = y;

		line = row(context, font, x, line, "Server enabled", snapshot.enabled() ? "yes" : "no");
		line = row(context, font, x, line, "OP bypass", snapshot.opBypass() ? "yes" : "no");
		line = row(context, font, x, line, "Operator", snapshot.operator() ? "yes" : "no");
		line += GuiTheme.sectionGap();

		for (AdminPermission permission : AdminPermission.values()) {
			boolean granted = snapshot.has(permission);
			int color = granted ? GuiTheme.SUCCESS : GuiTheme.DISABLED;
			context.text(font, Component.literal(granted ? "[x]" : "[ ]"), x, line, color);
			context.text(font, Component.literal(permission.id()), x + 26, line, granted ? GuiTheme.TEXT : GuiTheme.TEXT_MUTED);
			line += GuiTheme.rowHeight();
		}

		line += GuiTheme.sectionGap();
		context.text(font, Component.literal("admin.full implies every other permission."), x, line, GuiTheme.TEXT_MUTED);
		context.text(font, Component.literal("disguise.use additionally requires operator status."), x,
			line + GuiTheme.rowHeight(), GuiTheme.TEXT_MUTED);
	}

	private static int row(RenderContext context, Font font, int x, int y, String label, String value) {
		context.text(font, Component.literal(label), x, y, GuiTheme.TEXT_MUTED);
		context.text(font, Component.literal(value), x + 96, y, GuiTheme.TEXT);
		return y + GuiTheme.rowHeight();
	}
}
