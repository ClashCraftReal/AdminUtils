package com.lorenz.adminutils.disguise;

import com.lorenz.adminutils.AdminUtils;

/**
 * Holds the active disguise service. The default is a no-op so the rest of the mod
 * can depend on the interface without a null check.
 */
public final class DisguiseRegistry {
	private static volatile DisguiseService service = new NoopDisguiseService();

	private DisguiseRegistry() {
	}

	public static void initialize() {
		AdminUtils.LOGGER.info("Disguise service '{}' loaded (available={})", service.id(), service.isAvailable());
	}

	public static DisguiseService service() {
		return service;
	}

	public static boolean isAvailable() {
		return service.isAvailable();
	}

	public static void install(DisguiseService replacement) {
		if (replacement == null) {
			throw new IllegalArgumentException("Disguise service must not be null");
		}
		service = replacement;
		AdminUtils.LOGGER.info("Disguise service replaced with '{}' (available={})", replacement.id(), replacement.isAvailable());
	}
}
