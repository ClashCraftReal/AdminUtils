package com.lorenz.adminutils.client.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * On-disk root of the local item store. {@code dataVersion} gates migrations.
 */
public final class StorageDocument {
	public int dataVersion = ItemStorageService.CURRENT_DATA_VERSION;
	public List<StoredItem> items = new ArrayList<>();

	public StorageDocument() {
	}
}
