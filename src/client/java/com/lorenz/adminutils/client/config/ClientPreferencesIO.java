package com.lorenz.adminutils.client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.config.ConfigPaths;
import com.lorenz.adminutils.config.ConfigValidationException;
import com.lorenz.adminutils.config.JsonConfigFile;

/**
 * Loads and saves client UI preferences. A broken file is replaced by defaults in
 * memory rather than blocking the client from starting, and the unreadable file is
 * copied aside so the original data is never silently destroyed.
 */
public final class ClientPreferencesIO {
	private static volatile ClientPreferences current = new ClientPreferences();

	private ClientPreferencesIO() {
	}

	public static ClientPreferences get() {
		return current;
	}

	public static Path file() {
		return ConfigPaths.clientPreferencesFile();
	}

	public static void load() {
		try {
			current = JsonConfigFile.readOrCreate(file(), ClientPreferences.class,
				ClientPreferences::defaults, ClientPreferencesIO::validate);
			AdminUtils.LOGGER.info("Loaded client preferences (dataVersion {}) from {}", current.dataVersion, file());
		} catch (IOException | ConfigValidationException exception) {
			preserveUnreadableFile();
			current = ClientPreferences.defaults();
			AdminUtils.LOGGER.error("Failed to load client preferences from {}; using defaults", file(), exception);
		}
	}

	public static void save() {
		try {
			JsonConfigFile.write(file(), current);
		} catch (IOException exception) {
			AdminUtils.LOGGER.error("Failed to save client preferences to {}", file(), exception);
		}
	}

	/**
	 * Restores every preference to its default and writes the result immediately.
	 */
	public static void reset() {
		current.resetToDefaults();
		save();
	}

	/**
	 * Keeps a copy of a file that failed validation so a later save cannot erase the
	 * only remaining copy of the user's settings.
	 */
	private static void preserveUnreadableFile() {
		Path source = file();
		if (!Files.exists(source)) {
			return;
		}
		Path backup = source.resolveSibling(source.getFileName() + ".invalid");
		try {
			Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
			AdminUtils.LOGGER.warn("Preserved unreadable client preferences at {}", backup);
		} catch (IOException exception) {
			AdminUtils.LOGGER.error("Could not preserve unreadable client preferences at {}", backup, exception);
		}
	}

	static void validate(ClientPreferences preferences) throws ConfigValidationException {
		if (preferences.dataVersion > ClientPreferences.CURRENT_DATA_VERSION) {
			throw new ConfigValidationException("client-preferences.json dataVersion "
				+ preferences.dataVersion + " is newer than supported " + ClientPreferences.CURRENT_DATA_VERSION);
		}
		preferences.normalize();
	}
}
