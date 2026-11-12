package com.lorenz.adminutils.client.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.lorenz.adminutils.config.JsonConfigFile;

/**
 * JSON-backed local item store with atomic writes and versioned migrations.
 */
public final class JsonItemStorage implements ItemStorageService {
	private final Path file;
	private StorageDocument document;

	public JsonItemStorage(Path file) {
		this.file = file;
	}

	@Override
	public Path file() {
		return file;
	}

	@Override
	public synchronized StorageDocument load() throws IOException {
		try {
			StorageDocument loaded = JsonConfigFile.readOrCreate(file, StorageDocument.class,
				StorageDocument::new, document -> {
				});
			if (loaded.items == null) {
				loaded.items = new ArrayList<>();
			}
			StorageMigrations.migrate(loaded);
			this.document = loaded;
			return loaded;
		} catch (IOException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IOException("Unable to load item storage from " + file, exception);
		}
	}

	@Override
	public synchronized void save() throws IOException {
		JsonConfigFile.write(file, document());
	}

	@Override
	public synchronized List<StoredItem> items() {
		return List.copyOf(document().items);
	}

	@Override
	public synchronized void add(StoredItem item) throws IOException {
		if (item == null || item.id == null || item.id.isBlank()) {
			throw new IllegalArgumentException("Stored item requires an id");
		}
		document().items.add(item);
		save();
	}

	@Override
	public synchronized boolean remove(String id) throws IOException {
		boolean removed = document().items.removeIf(item -> item.id != null && item.id.equals(id));
		if (removed) {
			save();
		}
		return removed;
	}

	private StorageDocument document() {
		if (document == null) {
			document = new StorageDocument();
		}
		return document;
	}
}
