package com.lorenz.adminutils.permissions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.api.AdminPermission;
import com.lorenz.adminutils.config.ConfigValidationException;
import com.lorenz.adminutils.config.JsonConfigFile;

/**
 * Loads, validates and persists the server-side permission configuration.
 *
 * <p>Validation never throws away the whole file for a single bad entry: unusable
 * entries are skipped with a warning so a typo cannot lock every administrator out.
 * A structurally broken file does fall back to {@link #safeDefaults()}.
 */
public final class PermissionConfigIO {
	public static final int SCHEMA_VERSION = 1;

	private PermissionConfigIO() {
	}

	public static LoadedPermissions load(Path file) throws IOException, ConfigValidationException {
		PermissionConfig config = JsonConfigFile.readOrCreate(
			file,
			PermissionConfig.class,
			PermissionConfigIO::defaults,
			PermissionConfigIO::validateStructure);

		List<String> warnings = new ArrayList<>();
		Map<UUID, PermissionEntry> players = new LinkedHashMap<>();

		if (config.authorizedPlayers != null) {
			for (PermissionConfig.PlayerEntry entry : config.authorizedPlayers) {
				if (entry == null) {
					warnings.add("Skipped a null authorizedPlayers entry");
					continue;
				}

				UUID uuid = parseUuid(entry.uuid, warnings);
				if (uuid == null) {
					continue;
				}

				String name = entry.name == null ? "" : entry.name.trim();
				if (name.isEmpty()) {
					warnings.add("Player " + uuid + " has no display name");
				}

				Set<String> permissions = sanitizePermissions(uuid, entry.permissions, warnings);
				if (players.containsKey(uuid)) {
					warnings.add("Duplicate entry for " + uuid + "; the last one wins");
				}
				players.put(uuid, new PermissionEntry(uuid, name, permissions));
			}
		}

		return new LoadedPermissions(config.enabled, config.opBypass, players, List.copyOf(warnings));
	}

	public static LoadedPermissions safeDefaults() {
		return new LoadedPermissions(true, true, Map.of(), List.of("Using safe defaults: only operators have access"));
	}

	public static PermissionConfig defaults() {
		PermissionConfig config = new PermissionConfig();
		config.schemaVersion = SCHEMA_VERSION;
		config.enabled = true;
		config.opBypass = true;
		config.authorizedPlayers = new ArrayList<>();
		return config;
	}

	private static void validateStructure(PermissionConfig config) throws ConfigValidationException {
		if (config.schemaVersion > SCHEMA_VERSION) {
			throw new ConfigValidationException(
				"permissions.json schemaVersion " + config.schemaVersion + " is newer than supported " + SCHEMA_VERSION);
		}
		if (config.authorizedPlayers == null) {
			config.authorizedPlayers = new ArrayList<>();
		}
	}

	private static UUID parseUuid(String raw, List<String> warnings) {
		if (raw == null || raw.isBlank()) {
			warnings.add("Skipped an entry without a uuid");
			return null;
		}
		try {
			return UUID.fromString(raw.trim());
		} catch (IllegalArgumentException exception) {
			warnings.add("Skipped entry with invalid uuid '" + raw + "'");
			return null;
		}
	}

	private static Set<String> sanitizePermissions(UUID uuid, List<String> raw, List<String> warnings) {
		Set<String> permissions = new LinkedHashSet<>();
		if (raw == null) {
			return permissions;
		}
		for (String candidate : raw) {
			if (candidate == null || candidate.isBlank()) {
				continue;
			}
			String id = candidate.trim();
			if (AdminPermission.byId(id).isEmpty()) {
				warnings.add("Player " + uuid + " references unknown permission '" + id + "'");
				continue;
			}
			permissions.add(id);
		}
		return permissions;
	}

	/**
	 * Immutable, validated view of the configuration. This is what the runtime
	 * service swaps in atomically on reload.
	 */
	public record LoadedPermissions(boolean enabled, boolean opBypass, Map<UUID, PermissionEntry> players, List<String> warnings) {
		public LoadedPermissions {
			players = Collections.unmodifiableMap(new LinkedHashMap<>(players));
			warnings = List.copyOf(warnings);
		}

		public int authorizedCount() {
			return players.size();
		}

		public void logWarnings() {
			for (String warning : warnings) {
				AdminUtils.LOGGER.warn("permissions.json: {}", warning);
			}
		}
	}
}
