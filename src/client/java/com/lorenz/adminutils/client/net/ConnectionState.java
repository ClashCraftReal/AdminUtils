package com.lorenz.adminutils.client.net;

/**
 * Client-side view of the AdminUtils channel. {@code READY} means the handshake
 * succeeded and a permission snapshot is available.
 */
public enum ConnectionState {
	DISCONNECTED("Disconnected"),
	HANDSHAKING("Connecting"),
	READY("Connected"),
	INCOMPATIBLE("Incompatible"),
	ERROR("Error");

	private final String label;

	ConnectionState(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
