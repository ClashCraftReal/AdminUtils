package com.lorenz.adminutils.disguise;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

/**
 * Placeholder contract for a future disguise system.
 *
 * <p>Implementations must keep the original {@link UUID} and the server-side player
 * identity intact; only presentation may change. No identity concealment, moderation
 * bypass or packet rewriting is implemented in this phase.
 */
public interface DisguiseService {
	String id();

	boolean isAvailable();

	DisguiseSession openSession(ServerPlayer player);

	/**
	 * A single disguise lifetime. Closing it must restore the original presentation.
	 */
	interface DisguiseSession extends AutoCloseable {
		UUID originalUuid();

		boolean active();

		@Override
		void close();
	}
}
