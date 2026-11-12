package com.lorenz.adminutils.client.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies registered migrations in order. Version 1 is the initial schema, so the
 * chain is intentionally empty; later phases append entries here instead of editing
 * {@link StorageDocument} in place.
 */
public final class StorageMigrations {
	private static final List<StorageMigration> MIGRATIONS = new ArrayList<>();

	private StorageMigrations() {
	}

	public static void register(StorageMigration migration) {
		MIGRATIONS.add(migration);
		MIGRATIONS.sort(Comparator.comparingInt(StorageMigration::fromVersion));
	}

	public static void migrate(StorageDocument document) throws IOException {
		if (document.dataVersion > ItemStorageService.CURRENT_DATA_VERSION) {
			throw new IOException("Item storage dataVersion " + document.dataVersion
				+ " is newer than supported " + ItemStorageService.CURRENT_DATA_VERSION);
		}

		while (document.dataVersion < ItemStorageService.CURRENT_DATA_VERSION) {
			int from = document.dataVersion;
			StorageMigration next = MIGRATIONS.stream()
				.filter(migration -> migration.fromVersion() == from)
				.findFirst()
				.orElseThrow(() -> new IOException("No migration registered from dataVersion " + from));
			next.apply(document);
			document.dataVersion = next.toVersion();
		}
	}
}
