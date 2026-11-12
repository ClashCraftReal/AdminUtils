package com.lorenz.adminutils.client.config;

/**
 * Client-only UI preferences. Never sent to the server and never used for
 * authorization.
 */
public final class ClientPreferences {
	public static final int CURRENT_DATA_VERSION = 1;

	public int dataVersion = CURRENT_DATA_VERSION;
	public boolean showPermissionDetails = true;
	public boolean compactLayout = false;
	public String lastSection = "HOME";
}
