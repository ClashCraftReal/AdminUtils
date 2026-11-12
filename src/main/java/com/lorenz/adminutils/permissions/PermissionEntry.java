package com.lorenz.adminutils.permissions;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One authorized player as declared in {@code permissions.json}. Identity is the
 * UUID; the display name is informational only and is never used for lookups.
 */
public record PermissionEntry(UUID uuid, String name, Set<String> permissions) {
	public PermissionEntry {
		permissions = Collections.unmodifiableSet(new LinkedHashSet<>(permissions));
	}
}
