package com.lorenz.adminutils.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Shared JSON persistence helper. Writes are atomic so a crash mid-save cannot
 * leave a truncated configuration behind.
 */
public final class JsonConfigFile {
	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();

	private JsonConfigFile() {
	}

	public static Gson gson() {
		return GSON;
	}

	public static <T> T readOrCreate(Path path, Class<T> type, Supplier<T> defaults, ConfigValidator<T> validator)
		throws IOException, ConfigValidationException {
		if (!Files.exists(path)) {
			T value = defaults.get();
			validator.validate(value);
			write(path, value);
			return value;
		}

		String json = Files.readString(path, StandardCharsets.UTF_8);
		T value;
		try {
			value = GSON.fromJson(json, type);
		} catch (JsonParseException exception) {
			throw new ConfigValidationException("Malformed JSON in " + path.getFileName() + ": " + exception.getMessage(), exception);
		}

		if (value == null) {
			throw new ConfigValidationException("Configuration " + path.getFileName() + " is empty");
		}

		validator.validate(value);
		return value;
	}

	public static void write(Path path, Object value) throws IOException {
		Path parent = path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
		Files.writeString(temporary, GSON.toJson(value), StandardCharsets.UTF_8);

		try {
			Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
