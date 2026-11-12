package com.lorenz.adminutils.client.storage;

import java.io.IOException;

import com.lorenz.adminutils.config.JsonConfigFile;

/**
 * JSON import/export for {@link StorageDocument}. Import validates the data version
 * before handing the document back to the caller.
 */
public final class JsonImportExport implements ImportExport<StorageDocument> {
	public static final String FORMAT_ID = "adminutils.items.json";

	@Override
	public String formatId() {
		return FORMAT_ID;
	}

	@Override
	public String export(StorageDocument value) throws IOException {
		if (value == null) {
			throw new IOException("Nothing to export");
		}
		return JsonConfigFile.gson().toJson(value);
	}

	@Override
	public StorageDocument importFrom(String data) throws IOException {
		if (data == null || data.isBlank()) {
			throw new IOException("Import payload is empty");
		}

		StorageDocument document;
		try {
			document = JsonConfigFile.gson().fromJson(data, StorageDocument.class);
		} catch (RuntimeException exception) {
			throw new IOException("Malformed item storage JSON", exception);
		}

		if (document == null) {
			throw new IOException("Import payload is empty");
		}
		StorageMigrations.migrate(document);
		return document;
	}
}
