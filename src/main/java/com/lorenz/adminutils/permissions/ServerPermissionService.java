package com.lorenz.adminutils.permissions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.api.AdminPermission;
import com.lorenz.adminutils.api.PermissionSnapshot;
import com.lorenz.adminutils.config.ConfigValidationException;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * UUID-keyed permission store backed by {@code config/adminutils/permissions.json}.
 *
 * <p>State is held in an immutable record and swapped atomically on reload, so a
 * concurrent check always sees a consistent configuration.
 */
public final class ServerPermissionService implements PermissionService {
	private final Path file;
	private volatile PermissionConfigIO.LoadedPermissions state = PermissionConfigIO.safeDefaults();

	public ServerPermissionService(Path file) {
		this.file = file;
	}

	@Override
	public boolean isEnabled() {
		return state.enabled();
	}

	@Override
	public boolean isOpBypass() {
		return state.opBypass();
	}

	@Override
	public boolean has(UUID uuid, AdminPermission permission) {
		PermissionConfigIO.LoadedPermissions current = state;
		if (!current.enabled() || uuid == null) {
			return false;
		}
		return grantedByConfig(current, uuid, permission);
	}

	@Override
	public boolean has(ServerPlayer player, AdminPermission permission) {
		PermissionConfigIO.LoadedPermissions current = state;
		if (!current.enabled() || player == null) {
			return false;
		}

		boolean operator = isOperator(player);
		if (permission.isAdminOnly() && !operator) {
			return false;
		}
		if (current.opBypass() && operator) {
			return true;
		}
		return grantedByConfig(current, player.getUUID(), permission);
	}

	@Override
	public PermissionSnapshot snapshot(ServerPlayer player) {
		PermissionConfigIO.LoadedPermissions current = state;
		if (player == null) {
			return PermissionSnapshot.EMPTY;
		}

		boolean operator = isOperator(player);
		Set<String> granted = new LinkedHashSet<>();
		if (current.enabled()) {
			PermissionEntry entry = current.players().get(player.getUUID());
			if (entry != null) {
				granted.addAll(entry.permissions());
			}
			if (current.opBypass() && operator) {
				granted.add(AdminPermission.ADMIN_FULL.id());
			}
		}
		return new PermissionSnapshot(current.enabled(), operator, current.opBypass(), granted);
	}

	@Override
	public void reload() {
		try {
			PermissionConfigIO.LoadedPermissions loaded = PermissionConfigIO.load(file);
			state = loaded;
			loaded.logWarnings();
			AdminUtils.LOGGER.info("Loaded permissions from {}: enabled={}, opBypass={}, authorizedPlayers={}",
				file, loaded.enabled(), loaded.opBypass(), loaded.authorizedCount());
		} catch (IOException | ConfigValidationException exception) {
			state = PermissionConfigIO.safeDefaults();
			AdminUtils.LOGGER.error("Failed to load {}; falling back to safe defaults (operators only)",
				file, exception);
		}
	}

	@Override
	public int authorizedCount() {
		return state.authorizedCount();
	}

	public Path file() {
		return file;
	}

	private static boolean grantedByConfig(PermissionConfigIO.LoadedPermissions current, UUID uuid, AdminPermission permission) {
		PermissionEntry entry = current.players().get(uuid);
		if (entry == null) {
			return false;
		}
		if (entry.permissions().contains(AdminPermission.ADMIN_FULL.id())) {
			return true;
		}
		return entry.permissions().contains(permission.id());
	}

	/**
	 * Operator status comes from the live server-side player, never from a packet.
	 * {@code COMMANDS_GAMEMASTER} is vanilla permission level 2, the level required
	 * to run administrative commands.
	 */
	public static boolean isOperator(ServerPlayer player) {
		return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}
}
