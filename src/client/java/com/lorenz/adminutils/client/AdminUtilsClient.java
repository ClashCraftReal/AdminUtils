package com.lorenz.adminutils.client;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.client.config.ClientPreferencesIO;
import com.lorenz.adminutils.client.net.ClientNetworkHandler;
import com.lorenz.adminutils.client.storage.ItemStorageService;
import com.lorenz.adminutils.client.storage.JsonItemStorage;
import com.lorenz.adminutils.config.ConfigPaths;
import com.lorenz.adminutils.integration.librarian.LibrarianIntegration;

import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint. Wires client-only services and receivers; nothing here is
 * authoritative for permissions.
 */
public class AdminUtilsClient implements ClientModInitializer {
	private static ItemStorageService itemStorage;

	@Override
	public void onInitializeClient() {
		ClientPreferencesIO.load();
		ClientNetworkHandler.register();
		AdminKeybinds.register();

		itemStorage = new JsonItemStorage(ConfigPaths.itemStorageFile());
		try {
			itemStorage.load();
			AdminUtils.LOGGER.info("Local item storage ready at {} ({} entries, dataVersion {})",
				itemStorage.file(), itemStorage.items().size(), ItemStorageService.CURRENT_DATA_VERSION);
		} catch (Exception exception) {
			AdminUtils.LOGGER.error("Local item storage unavailable at {}", itemStorage.file(), exception);
		}

		AdminUtils.LOGGER.info("AdminUtils client initialized; {}", LibrarianIntegration.statusLine());
	}

	public static ItemStorageService itemStorage() {
		return itemStorage;
	}
}
