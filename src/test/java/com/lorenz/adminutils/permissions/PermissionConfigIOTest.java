package com.lorenz.adminutils.permissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.lorenz.adminutils.config.ConfigValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PermissionConfigIOTest {
	private static final UUID AUTHORIZED = UUID.fromString("11111111-2222-3333-4444-555555555555");
	private static final UUID FULL = UUID.fromString("66666666-7777-8888-9999-000000000000");

	@Test
	void createsDefaultsWhenFileIsMissing(@TempDir Path directory) throws Exception {
		Path file = directory.resolve("permissions.json");
		PermissionConfigIO.LoadedPermissions loaded = PermissionConfigIO.load(file);

		assertTrue(Files.exists(file));
		assertTrue(loaded.enabled());
		assertTrue(loaded.opBypass());
		assertEquals(0, loaded.authorizedCount());
		assertTrue(loaded.warnings().isEmpty());
	}

	@Test
	void loadsValidEntries(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{
			  "schemaVersion": 1,
			  "enabled": true,
			  "opBypass": false,
			  "authorizedPlayers": [
			    { "uuid": "11111111-2222-3333-4444-555555555555", "name": "Authorized", "permissions": ["gui.open", "players.view"] }
			  ]
			}
			""");

		PermissionConfigIO.LoadedPermissions loaded = PermissionConfigIO.load(file);

		assertTrue(loaded.enabled());
		assertFalse(loaded.opBypass());
		assertEquals(1, loaded.authorizedCount());
		PermissionEntry entry = loaded.players().get(AUTHORIZED);
		assertEquals("Authorized", entry.name());
		assertEquals(Set.of("gui.open", "players.view"), entry.permissions());
	}

	@Test
	void skipsInvalidUuidAndUnknownPermission(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{
			  "schemaVersion": 1,
			  "authorizedPlayers": [
			    { "uuid": "not-a-uuid", "name": "Broken", "permissions": ["gui.open"] },
			    { "uuid": "66666666-7777-8888-9999-000000000000", "name": "Full", "permissions": ["admin.full", "totally.unknown"] }
			  ]
			}
			""");

		PermissionConfigIO.LoadedPermissions loaded = PermissionConfigIO.load(file);

		assertEquals(1, loaded.authorizedCount());
		assertFalse(loaded.players().containsKey(AUTHORIZED));
		assertEquals(Set.of("admin.full"), loaded.players().get(FULL).permissions());
		assertEquals(2, loaded.warnings().size());
	}

	@Test
	void rejectsMalformedJson(@TempDir Path directory) throws Exception {
		Path file = write(directory, "{ \"enabled\": true, \"authorizedPlayers\": [ { \"uuid\": ");

		assertThrows(ConfigValidationException.class, () -> PermissionConfigIO.load(file));
	}

	@Test
	void rejectsNewerSchemaVersion(@TempDir Path directory) throws Exception {
		Path file = write(directory, "{ \"schemaVersion\": 99, \"authorizedPlayers\": [] }");

		assertThrows(ConfigValidationException.class, () -> PermissionConfigIO.load(file));
	}

	@Test
	void safeDefaultsGrantNothingByConfig() {
		PermissionConfigIO.LoadedPermissions defaults = PermissionConfigIO.safeDefaults();

		assertTrue(defaults.enabled());
		assertTrue(defaults.opBypass());
		assertEquals(0, defaults.authorizedCount());
		assertFalse(defaults.warnings().isEmpty());
	}

	@Test
	void duplicateEntriesKeepTheLastOne(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{
			  "authorizedPlayers": [
			    { "uuid": "11111111-2222-3333-4444-555555555555", "name": "First", "permissions": ["gui.open"] },
			    { "uuid": "11111111-2222-3333-4444-555555555555", "name": "Second", "permissions": ["players.view"] }
			  ]
			}
			""");

		PermissionConfigIO.LoadedPermissions loaded = PermissionConfigIO.load(file);

		assertEquals(1, loaded.authorizedCount());
		assertEquals("Second", loaded.players().get(AUTHORIZED).name());
		assertEquals(List.of("Duplicate entry for " + AUTHORIZED + "; the last one wins"), loaded.warnings());
	}

	@Test
	void acceptsPlayerManagementPermissions(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{
			  "schemaVersion": 1,
			  "enabled": true,
			  "opBypass": false,
			  "authorizedPlayers": [
			    {
			      "uuid": "11111111-2222-3333-4444-555555555555",
			      "name": "Inspector",
			      "permissions": [
			        "players.view",
			        "players.inventory.view",
			        "players.inventory.edit",
			        "players.enderchest.view",
			        "players.enderchest.edit"
			      ]
			    }
			  ]
			}
			""");

		PermissionConfigIO.LoadedPermissions loaded = PermissionConfigIO.load(file);

		assertEquals(1, loaded.authorizedCount());
		assertTrue(loaded.warnings().isEmpty());
		assertEquals(Set.of(
			"players.view",
			"players.inventory.view",
			"players.inventory.edit",
			"players.enderchest.view",
			"players.enderchest.edit"), loaded.players().get(AUTHORIZED).permissions());
	}

	@Test
	void playerManagementPermissionsAreNotGrantedImplicitly(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{
			  "schemaVersion": 1,
			  "authorizedPlayers": [
			    { "uuid": "11111111-2222-3333-4444-555555555555", "name": "Viewer", "permissions": ["players.view"] }
			  ]
			}
			""");

		PermissionConfigIO.LoadedPermissions loaded = PermissionConfigIO.load(file);

		assertEquals(Set.of("players.view"), loaded.players().get(AUTHORIZED).permissions());
		assertFalse(loaded.players().get(AUTHORIZED).permissions().contains("players.inventory.view"));
		assertFalse(loaded.players().get(AUTHORIZED).permissions().contains("players.inventory.edit"));
		assertFalse(loaded.players().get(AUTHORIZED).permissions().contains("players.enderchest.view"));
		assertFalse(loaded.players().get(AUTHORIZED).permissions().contains("players.enderchest.edit"));
	}

	@Test
	void keepsSchemaVersionOneForTheAdditivePermissionSet(@TempDir Path directory) throws Exception {
		Path file = write(directory, """
			{
			  "schemaVersion": 1,
			  "authorizedPlayers": [
			    { "uuid": "11111111-2222-3333-4444-555555555555", "name": "Legacy", "permissions": ["gui.open", "players.view"] }
			  ]
			}
			""");

		PermissionConfigIO.LoadedPermissions loaded = PermissionConfigIO.load(file);

		assertEquals(Set.of("gui.open", "players.view"), loaded.players().get(AUTHORIZED).permissions());
		assertTrue(loaded.warnings().isEmpty());
	}

	private static Path write(Path directory, String json) throws IOException {
		Path file = directory.resolve("permissions.json");
		Files.writeString(file, json, StandardCharsets.UTF_8);
		return file;
	}
}
