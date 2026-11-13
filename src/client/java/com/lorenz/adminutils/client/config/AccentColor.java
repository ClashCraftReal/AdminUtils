package com.lorenz.adminutils.client.config;

/**
 * Selectable accent colours. The identifier is what gets persisted, so renaming a
 * constant never invalidates an existing preferences file.
 */
public enum AccentColor {
	BLUE("blue", 0xFF3D7BDF),
	TEAL("teal", 0xFF2FA8A0),
	VIOLET("violet", 0xFF8A6BE0),
	AMBER("amber", 0xFFD79A2B),
	RED("red", 0xFFD9534F);

	public static final AccentColor DEFAULT = BLUE;

	private final String id;
	private final int argb;

	AccentColor(String id, int argb) {
		this.id = id;
		this.argb = argb;
	}

	public String id() {
		return id;
	}

	public int argb() {
		return argb;
	}

	/**
	 * Resolves a stored identifier. Unknown, blank and null values fall back to
	 * {@link #DEFAULT} so a hand-edited file can never break rendering.
	 */
	public static AccentColor byId(String id) {
		if (id != null) {
			for (AccentColor accent : values()) {
				if (accent.id.equalsIgnoreCase(id.trim())) {
					return accent;
				}
			}
		}
		return DEFAULT;
	}
}
