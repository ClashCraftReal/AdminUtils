package com.lorenz.adminutils.client.storage;

/**
 * One step in the storage migration chain. Migrations are applied in ascending
 * {@link #fromVersion()} order until the document reaches the current version.
 */
public interface StorageMigration {
	int fromVersion();

	int toVersion();

	void apply(StorageDocument document);
}
