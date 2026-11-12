package com.lorenz.adminutils.client.config;

import java.io.IOException;
import java.nio.file.Path;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.config.ConfigPaths;
import com.lorenz.adminutils.config.ConfigValidationException;
import com.lorenz.adminutils.config.JsonConfigFile;

/**
 * Loads and saves client UI preferences. A broken file is replaced by defaults rather
 * than blocking the client from starting.
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
				ClientPreferences::new, ClientPreferencesIO::validate);
			AdminUtils.LOGGER.info("Loaded client preferences from {}", file());
		} catch (IOException | ConfigValidationException exception) {
			current = new ClientPreferences();
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

	private static void validate(ClientPreferences preferences) throws ConfigValidationException {
		if (preferences.dataVersion > ClientPreferences.CURRENT_DATA_VERSION) {
			throw new ConfigValidationException("client-preferences.json dataVersion "
				+ preferences.dataVersion + " is newer than supported " + ClientPreferences.CURRENT_DATA_VERSION);
		}
		preferences.dataVersion = ClientPreferences.CURRENT_DATA_VERSION;
		if (preferences.lastSection == null || preferences.lastSection.isBlank()) {
			preferences.lastSection = "HOME";
		}
	}
}
