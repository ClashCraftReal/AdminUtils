package com.lorenz.adminutils.client.gui;

import com.lorenz.adminutils.api.AdminPermission;

/**
 * Top-level navigation entries. {@code placeholder} marks sections that exist in the
 * menu but have no implementation yet, so they render disabled instead of pretending
 * to work.
 */
public enum NavigationSection {
	HOME("Home", AdminPermission.GUI_OPEN, false),
	ITEMS("Items", AdminPermission.ITEMS_CREATE, true),
	PLAYERS("Players", AdminPermission.PLAYERS_VIEW, true),
	TOOLS("Tools", AdminPermission.ADMIN_FULL, true),
	PERMISSIONS("Permissions", AdminPermission.GUI_OPEN, false),
	SETTINGS("Settings", AdminPermission.GUI_OPEN, false);

	private final String label;
	private final AdminPermission permission;
	private final boolean placeholder;

	NavigationSection(String label, AdminPermission permission, boolean placeholder) {
		this.label = label;
		this.permission = permission;
		this.placeholder = placeholder;
	}

	public String label() {
		return label;
	}

	public AdminPermission permission() {
		return permission;
	}

	public boolean isPlaceholder() {
		return placeholder;
	}

	public static NavigationSection byName(String name) {
		for (NavigationSection section : values()) {
			if (section.name().equalsIgnoreCase(name)) {
				return section;
			}
		}
		return HOME;
	}
}
