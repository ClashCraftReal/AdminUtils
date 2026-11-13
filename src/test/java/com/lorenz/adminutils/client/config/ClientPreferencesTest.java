package com.lorenz.adminutils.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.lorenz.adminutils.config.ConfigValidationException;
import com.lorenz.adminutils.config.JsonConfigFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientPreferencesTest {
	@Test
	void defaultsAreVersionTwoAndValid() {
		ClientPreferences preferences = ClientPreferences.defaults();

		assertEquals(2, ClientPreferences.CURRENT_DATA_VERSION);
		assertEquals(2, preferences.dataVersion);
		assertTrue(preferences.showPermissionDetails);
		assertFalse(preferences.compactLayout);
		assertTrue(preferences.animations);
		assertEquals("blue", preferences.accentColor);
		assertEquals("HOME", preferences.lastSection);
		assertTrue(preferences.isDefault());
	}

	@Test
	void versionOneFileMigratesAndPreservesValues(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{
			  "dataVersion": 1,
			  "showPermissionDetails": false,
			  "compactLayout": true,
			  "lastSection": "ITEMS"
			}
			""");

		ClientPreferences preferences = read(file);

		assertEquals(2, preferences.dataVersion);
		assertFalse(preferences.showPermissionDetails);
		assertTrue(preferences.compactLayout);
		assertEquals("ITEMS", preferences.lastSection);
		assertTrue(preferences.animations);
		assertEquals("blue", preferences.accentColor);
	}

	@Test
	void versionTwoRoundTrips(@TempDir Path directory) throws Exception {
		Path file = directory.resolve("client-preferences.json");
		ClientPreferences saved = ClientPreferences.defaults();
		saved.showPermissionDetails = false;
		saved.compactLayout = true;
		saved.animations = false;
		saved.accentColor = "teal";
		saved.lastSection = "SETTINGS";
		JsonConfigFile.write(file, saved);

		ClientPreferences loaded = read(file);

		assertEquals(2, loaded.dataVersion);
		assertFalse(loaded.showPermissionDetails);
		assertTrue(loaded.compactLayout);
		assertFalse(loaded.animations);
		assertEquals("teal", loaded.accentColor);
		assertEquals("SETTINGS", loaded.lastSection);
		assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("\"dataVersion\": 2"));
	}

	@Test
	void unknownAccentFallsBackToDefault(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{ "dataVersion": 2, "accentColor": "chartreuse" }
			""");

		assertEquals("blue", read(file).accentColor);
	}

	@Test
	void nullAccentFallsBackToDefault(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{ "dataVersion": 2, "accentColor": null }
			""");

		assertEquals("blue", read(file).accentColor);
	}

	@Test
	void blankSectionFallsBackToDefault(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{ "dataVersion": 2, "lastSection": "  " }
			""");

		assertEquals("HOME", read(file).lastSection);
	}

	@Test
	void newerDataVersionIsRejected(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{ "dataVersion": 3 }
			""");

		assertThrows(ConfigValidationException.class, () -> read(file));
	}

	@Test
	void resetRestoresEveryDefault() {
		ClientPreferences preferences = ClientPreferences.defaults();
		preferences.showPermissionDetails = false;
		preferences.compactLayout = true;
		preferences.animations = false;
		preferences.accentColor = "red";
		preferences.lastSection = "TOOLS";
		assertFalse(preferences.isDefault());

		preferences.resetToDefaults();

		assertTrue(preferences.isDefault());
		assertEquals(2, preferences.dataVersion);
		assertTrue(preferences.showPermissionDetails);
		assertFalse(preferences.compactLayout);
		assertTrue(preferences.animations);
		assertEquals("blue", preferences.accentColor);
		assertEquals("HOME", preferences.lastSection);
	}

	@Test
	void isDefaultDetectsEveryChange() {
		ClientPreferences preferences = ClientPreferences.defaults();
		preferences.compactLayout = true;
		assertFalse(preferences.isDefault());

		preferences = ClientPreferences.defaults();
		preferences.animations = false;
		assertFalse(preferences.isDefault());

		preferences = ClientPreferences.defaults();
		preferences.showPermissionDetails = false;
		assertFalse(preferences.isDefault());

		preferences = ClientPreferences.defaults();
		preferences.accentColor = "amber";
		assertFalse(preferences.isDefault());
	}

	@Test
	void accentLookupIsCaseInsensitiveAndSafe() {
		assertEquals(AccentColor.TEAL, AccentColor.byId("TEAL"));
		assertEquals(AccentColor.TEAL, AccentColor.byId(" teal "));
		assertEquals(AccentColor.DEFAULT, AccentColor.byId("nope"));
		assertEquals(AccentColor.DEFAULT, AccentColor.byId(null));
		assertEquals(AccentColor.DEFAULT, AccentColor.byId(""));
	}

	@Test
	void accentIdentifiersAreUnique() {
		long distinct = java.util.Arrays.stream(AccentColor.values()).map(AccentColor::id).distinct().count();
		assertEquals(AccentColor.values().length, distinct);
	}

	@Test
	void normalizeIsIdempotent() {
		ClientPreferences preferences = ClientPreferences.defaults();
		preferences.accentColor = "VIOLET";
		preferences.normalize();
		preferences.normalize();

		assertEquals("violet", preferences.accentColor);
		assertEquals(2, preferences.dataVersion);
	}

	private static ClientPreferences read(Path file) throws IOException, ConfigValidationException {
		return JsonConfigFile.readOrCreate(file, ClientPreferences.class,
			ClientPreferences::defaults, ClientPreferencesIO::validate);
	}

	private static Path write(Path directory, String json) throws IOException {
		Path file = directory.resolve("client-preferences.json");
		Files.writeString(file, json, StandardCharsets.UTF_8);
		return file;
	}
}
