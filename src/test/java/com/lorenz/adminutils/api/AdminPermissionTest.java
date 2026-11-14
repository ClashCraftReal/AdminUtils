package com.lorenz.adminutils.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class AdminPermissionTest {
	@Test
	void exposesStableIdentifiers() {
		assertEquals("gui.open", AdminPermission.GUI_OPEN.id());
		assertEquals("items.create", AdminPermission.ITEMS_CREATE.id());
		assertEquals("items.edit", AdminPermission.ITEMS_EDIT.id());
		assertEquals("items.give", AdminPermission.ITEMS_GIVE.id());
		assertEquals("players.view", AdminPermission.PLAYERS_VIEW.id());
		assertEquals("players.manage", AdminPermission.PLAYERS_MANAGE.id());
		assertEquals("players.inventory.view", AdminPermission.PLAYERS_INVENTORY_VIEW.id());
		assertEquals("players.inventory.edit", AdminPermission.PLAYERS_INVENTORY_EDIT.id());
		assertEquals("players.enderchest.view", AdminPermission.PLAYERS_ENDERCHEST_VIEW.id());
		assertEquals("players.enderchest.edit", AdminPermission.PLAYERS_ENDERCHEST_EDIT.id());
		assertEquals("disguise.use", AdminPermission.DISGUISE_USE.id());
		assertEquals("admin.full", AdminPermission.ADMIN_FULL.id());
		assertEquals(12, AdminPermission.ids().size());
	}

	@Test
	void resolvesEveryPlayerManagementIdentifier() {
		assertEquals(AdminPermission.PLAYERS_INVENTORY_VIEW, AdminPermission.byId("players.inventory.view").orElseThrow());
		assertEquals(AdminPermission.PLAYERS_INVENTORY_EDIT, AdminPermission.byId("players.inventory.edit").orElseThrow());
		assertEquals(AdminPermission.PLAYERS_ENDERCHEST_VIEW, AdminPermission.byId("players.enderchest.view").orElseThrow());
		assertEquals(AdminPermission.PLAYERS_ENDERCHEST_EDIT, AdminPermission.byId("players.enderchest.edit").orElseThrow());
	}

	@Test
	void adminFullImpliesPlayerManagementPermissions() {
		assertTrue(AdminPermission.ADMIN_FULL.implies(AdminPermission.PLAYERS_INVENTORY_VIEW));
		assertTrue(AdminPermission.ADMIN_FULL.implies(AdminPermission.PLAYERS_INVENTORY_EDIT));
		assertTrue(AdminPermission.ADMIN_FULL.implies(AdminPermission.PLAYERS_ENDERCHEST_VIEW));
		assertTrue(AdminPermission.ADMIN_FULL.implies(AdminPermission.PLAYERS_ENDERCHEST_EDIT));
	}

	@Test
	void editPermissionsDoNotImplyViewPermissions() {
		assertFalse(AdminPermission.PLAYERS_INVENTORY_EDIT.implies(AdminPermission.PLAYERS_INVENTORY_VIEW));
		assertFalse(AdminPermission.PLAYERS_ENDERCHEST_EDIT.implies(AdminPermission.PLAYERS_ENDERCHEST_VIEW));
		assertFalse(AdminPermission.PLAYERS_INVENTORY_VIEW.implies(AdminPermission.PLAYERS_INVENTORY_EDIT));
		assertFalse(AdminPermission.PLAYERS_ENDERCHEST_VIEW.implies(AdminPermission.PLAYERS_ENDERCHEST_EDIT));
		assertFalse(AdminPermission.PLAYERS_INVENTORY_VIEW.implies(AdminPermission.PLAYERS_ENDERCHEST_VIEW));
		assertTrue(AdminPermission.PLAYERS_INVENTORY_EDIT.implies(AdminPermission.PLAYERS_INVENTORY_EDIT));
	}

	@Test
	void playerManagementPermissionsAreNotAdminOnly() {
		assertFalse(AdminPermission.PLAYERS_INVENTORY_VIEW.isAdminOnly());
		assertFalse(AdminPermission.PLAYERS_INVENTORY_EDIT.isAdminOnly());
		assertFalse(AdminPermission.PLAYERS_ENDERCHEST_VIEW.isAdminOnly());
		assertFalse(AdminPermission.PLAYERS_ENDERCHEST_EDIT.isAdminOnly());
		assertFalse(AdminPermission.PLAYERS_MANAGE.isAdminOnly());
	}

	@Test
	void adminFullImpliesEveryPermission() {
		for (AdminPermission permission : AdminPermission.values()) {
			assertTrue(AdminPermission.ADMIN_FULL.implies(permission), "admin.full must imply " + permission.id());
		}
	}

	@Test
	void otherPermissionsDoNotImplyEachOther() {
		assertFalse(AdminPermission.GUI_OPEN.implies(AdminPermission.PLAYERS_VIEW));
		assertFalse(AdminPermission.PLAYERS_VIEW.implies(AdminPermission.PLAYERS_MANAGE));
		assertFalse(AdminPermission.ITEMS_CREATE.implies(AdminPermission.ITEMS_GIVE));
		assertTrue(AdminPermission.GUI_OPEN.implies(AdminPermission.GUI_OPEN));
	}

	@Test
	void onlyDisguiseIsAdminOnly() {
		assertTrue(AdminPermission.DISGUISE_USE.isAdminOnly());
		assertFalse(AdminPermission.ADMIN_FULL.isAdminOnly());
		assertFalse(AdminPermission.GUI_OPEN.isAdminOnly());
		assertEquals(Set.of(AdminPermission.DISGUISE_USE),
			java.util.Arrays.stream(AdminPermission.values()).filter(AdminPermission::isAdminOnly)
				.collect(java.util.stream.Collectors.toSet()));
	}

	@Test
	void resolvesByIdentifier() {
		assertEquals(AdminPermission.PLAYERS_MANAGE, AdminPermission.byId("players.manage").orElseThrow());
		assertTrue(AdminPermission.byId("players.manage ").isEmpty());
		assertTrue(AdminPermission.byId("nope").isEmpty());
		assertTrue(AdminPermission.byId(null).isEmpty());
	}

	@Test
	void snapshotHonoursAdminFullAndDisabledFlag() {
		PermissionSnapshot full = new PermissionSnapshot(true, false, false, Set.of("admin.full"));
		assertTrue(full.has(AdminPermission.ITEMS_GIVE));
		assertTrue(full.has(AdminPermission.DISGUISE_USE));

		PermissionSnapshot limited = new PermissionSnapshot(true, false, false, Set.of("gui.open"));
		assertTrue(limited.has(AdminPermission.GUI_OPEN));
		assertFalse(limited.has(AdminPermission.PLAYERS_VIEW));

		PermissionSnapshot disabled = new PermissionSnapshot(false, true, true, Set.of("admin.full"));
		assertFalse(disabled.has(AdminPermission.GUI_OPEN));
	}

	@Test
	void snapshotCsvRoundTrips() {
		PermissionSnapshot snapshot = new PermissionSnapshot(true, true, true, Set.of("gui.open", "players.view"));
		assertEquals(Set.of("gui.open", "players.view"), PermissionSnapshot.parseCsv(snapshot.csv()));
		assertTrue(PermissionSnapshot.parseCsv("").isEmpty());
		assertTrue(PermissionSnapshot.parseCsv(null).isEmpty());
		assertEquals(Set.of("a", "b"), PermissionSnapshot.parseCsv(" a , , b "));
	}
}
