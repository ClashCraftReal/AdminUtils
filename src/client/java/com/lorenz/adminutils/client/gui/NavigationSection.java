package com.lorenz.adminutils.client.gui;

import com.lorenz.adminutils.api.AdminPermission;

/**
 * Top-level navigation entries. {@code placeholder} marks sections that exist in the
 * menu but have no implementation yet, so they render disabled instead of pretending
 * to work.
 *
 * <p>{@code local} marks sections that only read client-side state. Those stay
 * reachable when no permission snapshot is available, for example while disconnected
 * or on a server that does not run AdminUtils.
 */
public enum NavigationSection {
	HOME("Home", AdminPermission.GUI_OPEN, false, true),
	ITEMS("Items", AdminPermission.GUI_OPEN, true, true),
	PLAYERS("Players", AdminPermission.PLAYERS_VIEW, false, false),
	TOOLS("Tools", AdminPermission.ADMIN_FULL, true, false),
	PERMISSIONS("Permissions", AdminPermission.GUI_OPEN, false, true),
	SETTINGS("Settings", AdminPermission.GUI_OPEN, false, true);

	private final String label;
	private final AdminPermission permission;
	private final boolean placeholder;
	private final boolean local;

	NavigationSection(String label, AdminPermission permission, boolean placeholder, boolean local) {
		this.label = label;
		this.permission = permission;
		this.placeholder = placeholder;
		this.local = local;
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

	/**
	 * True when the section works without a server permission snapshot.
	 */
	public boolean isLocal() {
		return local;
	}

	/**
	 * Whether the section may be opened with the given snapshot. Local sections are
	 * always reachable; the rest require the server to grant their permission.
	 */
	public boolean isAccessible(boolean permitted) {
		return local || permitted;
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
