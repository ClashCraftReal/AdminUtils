package com.lorenz.adminutils.net;

import com.lorenz.adminutils.AdminUtils;

import net.minecraft.resources.Identifier;

/**
 * Wire contract shared by client and server. Bump {@link #VERSION} whenever a
 * payload shape or permission identifier changes; mismatched peers are rejected
 * during the handshake instead of failing later with a decode error.
 */
public final class Protocol {
	public static final int VERSION = 1;

	public static final int MAX_REQUEST_ID_LENGTH = 64;
	public static final int MAX_ACTION_ID_LENGTH = 64;
	public static final int MAX_ARGUMENT_LENGTH = 128;
	public static final int MAX_VERSION_STRING_LENGTH = 64;

	private Protocol() {
	}

	public static Identifier channel(String path) {
		return AdminUtils.id(path);
	}
}
