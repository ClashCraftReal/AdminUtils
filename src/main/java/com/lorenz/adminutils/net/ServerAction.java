package com.lorenz.adminutils.net;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.lorenz.adminutils.api.AdminPermission;

/**
 * Server-side allow-list of privileged actions. A client may only name one of these
 * identifiers; the required permission is resolved here and never sent by the client.
 */
public enum ServerAction {
	PERMISSIONS_REFRESH("permissions.refresh", AdminPermission.GUI_OPEN),
	DISGUISE_STATUS("disguise.status", AdminPermission.DISGUISE_USE);

	private static final Map<String, ServerAction> BY_ID;

	static {
		Map<String, ServerAction> byId = new LinkedHashMap<>();
		for (ServerAction action : values()) {
			byId.put(action.id, action);
		}
		BY_ID = Collections.unmodifiableMap(byId);
	}

	private final String id;
	private final AdminPermission permission;

	ServerAction(String id, AdminPermission permission) {
		this.id = id;
		this.permission = permission;
	}

	public String id() {
		return id;
	}

	public AdminPermission permission() {
		return permission;
	}

	public static Optional<ServerAction> byId(String id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(BY_ID.get(id));
	}
}
