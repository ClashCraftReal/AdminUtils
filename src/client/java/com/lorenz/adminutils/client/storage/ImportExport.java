package com.lorenz.adminutils.client.storage;

import java.io.IOException;

/**
 * Import/export boundary for local storage. Kept format-agnostic so a future binary or
 * compressed format can be added without touching call sites.
 */
public interface ImportExport<T> {
	String formatId();

	String export(T value) throws IOException;

	T importFrom(String data) throws IOException;
}
