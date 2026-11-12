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
		assertEquals("disguise.use", AdminPermission.DISGUISE_USE.id());
		assertEquals("admin.full", AdminPermission.ADMIN_FULL.id());
		assertEquals(8, AdminPermission.ids().size());
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
