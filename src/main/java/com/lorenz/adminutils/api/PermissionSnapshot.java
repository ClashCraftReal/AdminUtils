package com.lorenz.adminutils.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A read-only view of what the server currently allows a player to do.
 *
 * <p>This snapshot is advisory only. The client uses it to grey out unavailable
 * features; the server re-checks every privileged action independently and never
 * trusts the values it previously sent.
 */
public record PermissionSnapshot(boolean enabled, boolean operator, boolean opBypass, Set<String> granted) {
	public static final PermissionSnapshot EMPTY = new PermissionSnapshot(false, false, false, Set.of());

	public PermissionSnapshot {
		granted = Collections.unmodifiableSet(new LinkedHashSet<>(granted));
	}

	public boolean has(AdminPermission permission) {
		if (!enabled) {
			return false;
		}
		if (granted.contains(AdminPermission.ADMIN_FULL.id())) {
			return true;
		}
		return granted.contains(permission.id());
	}

	public boolean isEmpty() {
		return granted.isEmpty();
	}

	public String csv() {
		return String.join(",", granted);
	}

	public static Set<String> parseCsv(String csv) {
		Set<String> result = new LinkedHashSet<>();
		if (csv == null || csv.isBlank()) {
			return result;
		}
		for (String part : csv.split(",")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				result.add(trimmed);
			}
		}
		return result;
	}
}
