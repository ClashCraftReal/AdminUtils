package com.lorenz.adminutils.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every privileged action AdminUtils can perform. The identifiers are the stable
 * contract used by {@code config/adminutils/permissions.json} and by the network
 * protocol, so they must never be renamed without a protocol bump.
 */
public enum AdminPermission {
	GUI_OPEN("gui.open"),
	ITEMS_CREATE("items.create"),
	ITEMS_EDIT("items.edit"),
	ITEMS_GIVE("items.give"),
	PLAYERS_VIEW("players.view"),
	PLAYERS_MANAGE("players.manage"),
	DISGUISE_USE("disguise.use"),
	ADMIN_FULL("admin.full");

	/** Permissions that additionally require operator status, regardless of grants. */
	private static final Set<AdminPermission> ADMIN_ONLY = Set.of(DISGUISE_USE);

	private static final Map<String, AdminPermission> BY_ID;

	static {
		Map<String, AdminPermission> byId = new LinkedHashMap<>();
		for (AdminPermission permission : values()) {
			byId.put(permission.id, permission);
		}
		BY_ID = Collections.unmodifiableMap(byId);
	}

	private final String id;

	AdminPermission(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public boolean isAdminOnly() {
		return ADMIN_ONLY.contains(this);
	}

	/**
	 * {@code admin.full} inherits every other permission. No other permission implies another.
	 */
	public boolean implies(AdminPermission other) {
		return this == other || this == ADMIN_FULL;
	}

	public static Optional<AdminPermission> byId(String id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(BY_ID.get(id));
	}

	public static Set<String> ids() {
		return BY_ID.keySet();
	}
}
