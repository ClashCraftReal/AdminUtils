package com.lorenz.adminutils.permissions;

import java.util.UUID;

import com.lorenz.adminutils.api.AdminPermission;
import com.lorenz.adminutils.api.PermissionSnapshot;

import net.minecraft.server.level.ServerPlayer;

/**
 * Authoritative permission checks. Implementations must never accept a UUID or a
 * permission set supplied by a client.
 */
public interface PermissionService {
	boolean isEnabled();

	boolean isOpBypass();

	/**
	 * Config-grant check only. Operator status cannot be evaluated without a live
	 * player, so this never returns {@code true} because of OP bypass.
	 */
	boolean has(UUID uuid, AdminPermission permission);

	/**
	 * Full check against the authenticated server-side player, including OP bypass
	 * and admin-only restrictions.
	 */
	boolean has(ServerPlayer player, AdminPermission permission);

	PermissionSnapshot snapshot(ServerPlayer player);

	void reload();

	int authorizedCount();
}
