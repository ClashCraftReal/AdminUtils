package com.lorenz.adminutils.permissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lorenz.adminutils.api.AdminPermission;
import com.lorenz.adminutils.api.PermissionSnapshot;

import org.junit.jupiter.api.Test;

/**
 * Regression tests for OP detection and the opBypass rule. These cover the reported
 * bug where an operator saw "Operator: no / Permissions: none".
 */
class ServerPermissionServiceTest {
	private static final UUID OPERATOR = UUID.fromString("f3d28cb0-7225-3cb1-baeb-2dadd2be89ae");
	private static final UUID AUTHORIZED = UUID.fromString("11111111-2222-3333-4444-555555555555");
	private static final UUID STRANGER = UUID.fromString("99999999-8888-7777-6666-555555555555");

	@Test
	void operatorWithBypassEnabledGetsAdminFull() {
		PermissionConfigIO.LoadedPermissions config = config(true, Map.of());

		PermissionSnapshot snapshot = ServerPermissionService.snapshotFor(config, OPERATOR, true);

		assertTrue(snapshot.operator());
		assertTrue(snapshot.opBypass());
		assertTrue(snapshot.has(AdminPermission.ADMIN_FULL));
		assertTrue(snapshot.has(AdminPermission.GUI_OPEN));
		assertTrue(snapshot.has(AdminPermission.ITEMS_GIVE));
		assertTrue(snapshot.has(AdminPermission.DISGUISE_USE));
		assertEquals(Set.of("admin.full"), snapshot.granted());
	}

	@Test
	void operatorWithBypassDisabledGetsNothingWithoutAnEntry() {
		PermissionConfigIO.LoadedPermissions config = config(false, Map.of());

		PermissionSnapshot snapshot = ServerPermissionService.snapshotFor(config, OPERATOR, true);

		assertTrue(snapshot.operator());
		assertFalse(snapshot.opBypass());
		assertTrue(snapshot.granted().isEmpty());
		assertFalse(snapshot.has(AdminPermission.GUI_OPEN));
		assertFalse(ServerPermissionService.hasFor(config, OPERATOR, true, AdminPermission.GUI_OPEN));
	}

	@Test
	void operatorWithBypassDisabledKeepsExplicitGrants() {
		PermissionConfigIO.LoadedPermissions config = config(false, Map.of(
			OPERATOR, new PermissionEntry(OPERATOR, "Tester", Set.of("gui.open"))));

		PermissionSnapshot snapshot = ServerPermissionService.snapshotFor(config, OPERATOR, true);

		assertEquals(Set.of("gui.open"), snapshot.granted());
		assertTrue(ServerPermissionService.hasFor(config, OPERATOR, true, AdminPermission.GUI_OPEN));
		assertFalse(ServerPermissionService.hasFor(config, OPERATOR, true, AdminPermission.PLAYERS_VIEW));
	}

	@Test
	void nonOperatorWithBypassEnabledGetsOnlyConfiguredPermissions() {
		PermissionConfigIO.LoadedPermissions config = config(true, Map.of(
			AUTHORIZED, new PermissionEntry(AUTHORIZED, "Authorized", Set.of("gui.open", "players.view"))));

		PermissionSnapshot snapshot = ServerPermissionService.snapshotFor(config, AUTHORIZED, false);

		assertFalse(snapshot.operator());
		assertEquals(Set.of("gui.open", "players.view"), snapshot.granted());
		assertTrue(snapshot.has(AdminPermission.PLAYERS_VIEW));
		assertFalse(snapshot.has(AdminPermission.ADMIN_FULL));
		assertFalse(snapshot.has(AdminPermission.DISGUISE_USE));
	}

	@Test
	void strangerGetsNothingEvenWithBypassEnabled() {
		PermissionConfigIO.LoadedPermissions config = config(true, Map.of());

		PermissionSnapshot snapshot = ServerPermissionService.snapshotFor(config, STRANGER, false);

		assertFalse(snapshot.operator());
		assertTrue(snapshot.granted().isEmpty());
		assertFalse(ServerPermissionService.hasFor(config, STRANGER, false, AdminPermission.GUI_OPEN));
	}

	@Test
	void adminOnlyPermissionRequiresOperatorEvenWhenGranted() {
		PermissionConfigIO.LoadedPermissions config = config(false, Map.of(
			AUTHORIZED, new PermissionEntry(AUTHORIZED, "Authorized", Set.of("disguise.use"))));

		assertFalse(ServerPermissionService.hasFor(config, AUTHORIZED, false, AdminPermission.DISGUISE_USE));
		assertTrue(ServerPermissionService.hasFor(config, AUTHORIZED, true, AdminPermission.DISGUISE_USE));
	}

	@Test
	void disabledServerGrantsNothingButStillReportsOperator() {
		PermissionConfigIO.LoadedPermissions config = new PermissionConfigIO.LoadedPermissions(
			false, true, Map.of(), List.of());

		PermissionSnapshot snapshot = ServerPermissionService.snapshotFor(config, OPERATOR, true);

		assertFalse(snapshot.enabled());
		assertTrue(snapshot.operator());
		assertTrue(snapshot.granted().isEmpty());
		assertFalse(snapshot.has(AdminPermission.GUI_OPEN));
		assertFalse(ServerPermissionService.hasFor(config, OPERATOR, true, AdminPermission.GUI_OPEN));
	}

	@Test
	void bypassDoesNotMutateStoredEntries() {
		Map<UUID, PermissionEntry> players = new LinkedHashMap<>();
		players.put(AUTHORIZED, new PermissionEntry(AUTHORIZED, "Authorized", Set.of("gui.open")));
		PermissionConfigIO.LoadedPermissions config = config(true, players);

		ServerPermissionService.snapshotFor(config, OPERATOR, true);
		ServerPermissionService.snapshotFor(config, AUTHORIZED, false);

		assertEquals(Set.of("gui.open"), config.players().get(AUTHORIZED).permissions());
		assertFalse(config.players().containsKey(OPERATOR));
		assertEquals(1, config.authorizedCount());
	}

	@Test
	void nullIdentityIsRejected() {
		PermissionConfigIO.LoadedPermissions config = config(true, Map.of());

		assertFalse(ServerPermissionService.hasFor(config, null, true, AdminPermission.GUI_OPEN));
		assertFalse(ServerPermissionService.hasFor(config, OPERATOR, true, null));
		assertTrue(ServerPermissionService.snapshotFor(config, null, true).granted().isEmpty());
	}

	private static PermissionConfigIO.LoadedPermissions config(boolean opBypass, Map<UUID, PermissionEntry> players) {
		return new PermissionConfigIO.LoadedPermissions(true, opBypass, players, List.of());
	}
}
