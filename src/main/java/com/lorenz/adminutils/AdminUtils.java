package com.lorenz.adminutils;

import com.lorenz.adminutils.audit.AuditLog;
import com.lorenz.adminutils.config.ConfigPaths;
import com.lorenz.adminutils.disguise.DisguiseRegistry;
import com.lorenz.adminutils.integration.librarian.LibrarianIntegration;
import com.lorenz.adminutils.net.PayloadRegistry;
import com.lorenz.adminutils.net.Protocol;
import com.lorenz.adminutils.net.ServerNetworkHandler;
import com.lorenz.adminutils.permissions.PermissionEvents;
import com.lorenz.adminutils.permissions.ServerPermissionService;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminUtils implements ModInitializer {
	public static final String MOD_ID = "adminutils";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ServerPermissionService permissions;
	private static AuditLog auditLog;

	@Override
	public void onInitialize() {
		long startedAt = System.nanoTime();

		LOGGER.info("AdminUtils {} starting on Minecraft {} (protocol v{})",
			modVersion(), minecraftVersion(), Protocol.VERSION);

		auditLog = new AuditLog(ConfigPaths.auditFile());
		permissions = new ServerPermissionService(ConfigPaths.permissionsFile());
		permissions.reload();

		PayloadRegistry.register();
		ServerNetworkHandler.register(permissions, auditLog);
		PermissionEvents.register(permissions, auditLog);

		DisguiseRegistry.initialize();
		LibrarianIntegration.logStatus();

		LOGGER.info("AdminUtils ready in {} ms: permissions enabled={}, opBypass={}, authorizedPlayers={}",
			(System.nanoTime() - startedAt) / 1_000_000L,
			permissions.isEnabled(), permissions.isOpBypass(), permissions.authorizedCount());
	}

	public static ServerPermissionService permissions() {
		return permissions;
	}

	public static AuditLog auditLog() {
		return auditLog;
	}

	public static String modVersion() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}

	public static String minecraftVersion() {
		return FabricLoader.getInstance().getModContainer("minecraft")
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
