package com.lorenz.adminutils.client.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonItemStorageTest {
	@Test
	void createsEmptyDocumentOnFirstLoad(@TempDir Path directory) throws Exception {
		JsonItemStorage storage = new JsonItemStorage(directory.resolve("items.json"));

		StorageDocument document = storage.load();

		assertEquals(ItemStorageService.CURRENT_DATA_VERSION, document.dataVersion);
		assertTrue(document.items.isEmpty());
		assertTrue(storage.items().isEmpty());
	}

	@Test
	void persistsAddedItems(@TempDir Path directory) throws Exception {
		Path file = directory.resolve("items.json");
		JsonItemStorage storage = new JsonItemStorage(file);
		storage.load();

		storage.add(new StoredItem("sword", "Sword", "{\"damage\":7}", 1L));

		JsonItemStorage reopened = new JsonItemStorage(file);
		reopened.load();
		List<StoredItem> items = reopened.items();

		assertEquals(1, items.size());
		assertEquals("sword", items.get(0).id);
		assertEquals("{\"damage\":7}", items.get(0).payload);
	}

	@Test
	void removesById(@TempDir Path directory) throws Exception {
		JsonItemStorage storage = new JsonItemStorage(directory.resolve("items.json"));
		storage.load();
		storage.add(new StoredItem("a", "A", "{}", 1L));
		storage.add(new StoredItem("b", "B", "{}", 2L));

		assertTrue(storage.remove("a"));
		assertFalse(storage.remove("missing"));
		assertEquals(1, storage.items().size());
		assertEquals("b", storage.items().get(0).id);
	}

	@Test
	void rejectsItemWithoutId(@TempDir Path directory) throws Exception {
		JsonItemStorage storage = new JsonItemStorage(directory.resolve("items.json"));
		storage.load();

		assertThrows(IllegalArgumentException.class, () -> storage.add(new StoredItem(null, "x", "{}", 0L)));
	}

	@Test
	void rejectsNewerDataVersion(@TempDir Path directory) throws Exception {
		StorageDocument document = new StorageDocument();
		document.dataVersion = ItemStorageService.CURRENT_DATA_VERSION + 1;

		assertThrows(IOException.class, () -> StorageMigrations.migrate(document));
	}

	@Test
	void importExportRoundTrips(@TempDir Path directory) throws Exception {
		StorageDocument document = new StorageDocument();
		document.items.add(new StoredItem("id", "Name", "{}", 5L));

		JsonImportExport codec = new JsonImportExport();
		String exported = codec.export(document);
		StorageDocument imported = codec.importFrom(exported);

		assertEquals(JsonImportExport.FORMAT_ID, codec.formatId());
		assertEquals(ItemStorageService.CURRENT_DATA_VERSION, imported.dataVersion);
		assertEquals(1, imported.items.size());
		assertEquals("id", imported.items.get(0).id);
	}

	@Test
	void importRejectsEmptyAndMalformedPayloads() {
		JsonImportExport codec = new JsonImportExport();

		assertThrows(IOException.class, () -> codec.importFrom(""));
		assertThrows(IOException.class, () -> codec.importFrom("   "));
		assertThrows(IOException.class, () -> codec.importFrom("{ not json"));
		assertThrows(IOException.class, () -> codec.export(null));
	}
}
