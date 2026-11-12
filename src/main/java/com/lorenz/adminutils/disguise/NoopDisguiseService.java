package com.lorenz.adminutils.disguise;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

/**
 * Default service. It never changes anything and reports itself as unavailable, so
 * callers can wire the disguise path now and enable it in a later phase.
 */
public final class NoopDisguiseService implements DisguiseService {
	public static final String ID = "noop";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public DisguiseSession openSession(ServerPlayer player) {
		return new InactiveSession(player.getUUID());
	}

	private record InactiveSession(UUID originalUuid) implements DisguiseSession {
		@Override
		public boolean active() {
			return false;
		}

		@Override
		public void close() {
		}
	}
}
