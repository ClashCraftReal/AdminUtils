package com.lorenz.adminutils.integration.librarian;

import java.util.Optional;

import com.lorenz.adminutils.AdminUtils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * Optional, one-way detection of the Librarian mod.
 *
 * <p>Librarian is a client-side saved-hotbar replacement with no published
 * integration API, entrypoint or documented data contract. AdminUtils therefore only
 * detects its presence and never reads or writes its files. Hotbar synchronisation
 * belongs to a later phase and must be built on a real API, not on assumptions.
 */
public final class LibrarianIntegration {
	public static final String MOD_ID = "librarian";

	private LibrarianIntegration() {
	}

	public static boolean isInstalled() {
		return FabricLoader.getInstance().isModLoaded(MOD_ID);
	}

	public static Optional<String> version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(ModContainer::getMetadata)
			.map(metadata -> metadata.getVersion().getFriendlyString());
	}

	public static String statusLine() {
		if (!isInstalled()) {
			return "Librarian not installed";
		}
		return "Librarian " + version().orElse("unknown") + " detected (integration inactive)";
	}

	public static void logStatus() {
		AdminUtils.LOGGER.info("Librarian integration: {}", statusLine());
	}
}
