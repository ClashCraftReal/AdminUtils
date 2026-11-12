package com.lorenz.adminutils.config;

import java.nio.file.Path;

import com.lorenz.adminutils.AdminUtils;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Single source of truth for on-disk locations. Server-only files live under the
 * server's config directory and are never sent to clients.
 */
public final class ConfigPaths {
	private ConfigPaths() {
	}

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve(AdminUtils.MOD_ID);
	}

	public static Path permissionsFile() {
		return directory().resolve("permissions.json");
	}

	public static Path auditFile() {
		return directory().resolve("audit.log");
	}

	public static Path clientPreferencesFile() {
		return directory().resolve("client-preferences.json");
	}

	public static Path itemStorageFile() {
		return directory().resolve("items.json");
	}
}
