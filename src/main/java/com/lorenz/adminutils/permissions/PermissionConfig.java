package com.lorenz.adminutils.permissions;

import java.util.ArrayList;
import java.util.List;

/**
 * Gson binding for {@code config/adminutils/permissions.json}. Field names and
 * defaults are part of the on-disk contract; see {@link PermissionConfigIO} for
 * validation.
 */
public final class PermissionConfig {
	public int schemaVersion = PermissionConfigIO.SCHEMA_VERSION;
	public boolean enabled = true;
	public boolean opBypass = true;
	public List<PlayerEntry> authorizedPlayers = new ArrayList<>();

	public static final class PlayerEntry {
		public String uuid;
		public String name;
		public List<String> permissions = new ArrayList<>();
	}
}
