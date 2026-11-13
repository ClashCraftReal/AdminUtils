package com.lorenz.adminutils.client.config;

/**
 * Client-only UI preferences. Never sent to the server and never used for
 * authorization.
 *
 * <p>Data version 2 added {@link #accentColor} and {@link #animations}. A version 1
 * file has neither field, so Gson leaves the field initialisers below in place and the
 * existing values are preserved; {@link #normalize()} then stamps the new version.
 */
public final class ClientPreferences {
	public static final int CURRENT_DATA_VERSION = 2;
	public static final String DEFAULT_LAST_SECTION = "HOME";

	public int dataVersion = CURRENT_DATA_VERSION;
	public boolean showPermissionDetails = true;
	public boolean compactLayout = false;
	public boolean animations = true;
	public String accentColor = AccentColor.DEFAULT.id();
	public String lastSection = DEFAULT_LAST_SECTION;

	public static ClientPreferences defaults() {
		return new ClientPreferences();
	}

	/**
	 * Clamps the loaded document to the current schema. Unknown accent identifiers and
	 * blank section names are replaced with defaults instead of failing the load.
	 */
	public void normalize() {
		dataVersion = CURRENT_DATA_VERSION;
		accentColor = AccentColor.byId(accentColor).id();
		if (lastSection == null || lastSection.isBlank()) {
			lastSection = DEFAULT_LAST_SECTION;
		}
	}

	/**
	 * Copies defaults into this instance so callers holding a reference observe the
	 * reset without re-reading the file.
	 */
	public void resetToDefaults() {
		ClientPreferences defaults = defaults();
		this.dataVersion = defaults.dataVersion;
		this.showPermissionDetails = defaults.showPermissionDetails;
		this.compactLayout = defaults.compactLayout;
		this.animations = defaults.animations;
		this.accentColor = defaults.accentColor;
		this.lastSection = defaults.lastSection;
	}

	public boolean isDefault() {
		return dataVersion == CURRENT_DATA_VERSION
			&& showPermissionDetails
			&& !compactLayout
			&& animations
			&& AccentColor.DEFAULT.id().equals(accentColor)
			&& DEFAULT_LAST_SECTION.equals(lastSection);
	}
}
