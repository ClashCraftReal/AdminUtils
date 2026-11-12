package com.lorenz.adminutils.client.storage;

/**
 * One stored entry. {@code payload} is opaque to this layer: phase 1 stores whatever
 * a future item editor produces without interpreting it.
 */
public final class StoredItem {
	public String id;
	public String name;
	public String payload;
	public long createdAtEpochMillis;

	public StoredItem() {
	}

	public StoredItem(String id, String name, String payload, long createdAtEpochMillis) {
		this.id = id;
		this.name = name;
		this.payload = payload;
		this.createdAtEpochMillis = createdAtEpochMillis;
	}
}
