package com.lorenz.adminutils.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.config.JsonConfigFile;

import net.minecraft.server.level.ServerPlayer;

/**
 * Append-only administrative action log. One JSON object per line so the file can
 * be tailed, grepped or ingested without a parser.
 *
 * <p>Logging failures are reported but never propagate: an unwritable audit file
 * must not break an administrative action that already succeeded.
 */
public final class AuditLog {
	private final Path file;
	private final Object lock = new Object();

	public AuditLog(Path file) {
		this.file = file;
	}

	public void record(String action, ServerPlayer actor, String detail, boolean success) {
		UUID actorId = actor == null ? null : actor.getUUID();
		String actorName = actor == null ? "server" : actor.getGameProfile().name();
		record(action, actorId, actorName, detail, success);
	}

	public void record(String action, UUID actorId, String actorName, String detail, boolean success) {
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("timestamp", Instant.now().toString());
		entry.put("action", action);
		entry.put("actor", actorId == null ? null : actorId.toString());
		entry.put("actorName", actorName);
		entry.put("success", success);
		entry.put("detail", detail);

		String line = JsonConfigFile.gson().toJson(entry);
		AdminUtils.LOGGER.info("audit {} {} by {} ({}): {}", success ? "OK" : "DENIED", action, actorName, actorId, detail);

		synchronized (lock) {
			try {
				Path parent = file.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException exception) {
				AdminUtils.LOGGER.error("Unable to append to audit log {}", file, exception);
			}
		}
	}

	public Path file() {
		return file;
	}
}
