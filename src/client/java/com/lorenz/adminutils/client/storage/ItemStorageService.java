package com.lorenz.adminutils.client.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Local item storage boundary. Implementations must never talk to the server; this is
 * a client-side cache for a future item tool.
 */
public interface ItemStorageService {
	int CURRENT_DATA_VERSION = 1;

	Path file();

	StorageDocument load() throws IOException;

	void save() throws IOException;

	List<StoredItem> items();

	void add(StoredItem item) throws IOException;

	boolean remove(String id) throws IOException;
}
