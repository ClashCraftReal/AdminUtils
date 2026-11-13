package com.lorenz.adminutils.client.gui;

import com.lorenz.adminutils.client.config.ClientPreferencesIO;

/**
 * Gate for the few decorative animations the menu uses. When the preference is off,
 * {@link #progress(long, int)} reports completion immediately so every call site
 * renders its final state without a separate code path.
 */
public final class GuiAnimation {
	public static final int FAST_MILLIS = 150;

	private GuiAnimation() {
	}

	public static boolean enabled() {
		return ClientPreferencesIO.get().animations;
	}

	/**
	 * Eased-in progress in {@code [0, 1]} for an animation that started at
	 * {@code startMillis}. Returns {@code 1} when animations are disabled.
	 */
	public static float progress(long startMillis, int durationMillis) {
		if (!enabled() || durationMillis <= 0) {
			return 1.0F;
		}
		long elapsed = System.currentTimeMillis() - startMillis;
		if (elapsed <= 0L) {
			return 0.0F;
		}
		if (elapsed >= durationMillis) {
			return 1.0F;
		}
		float linear = elapsed / (float) durationMillis;
		return linear * linear;
	}

	/**
	 * Scales the alpha channel of an ARGB colour, leaving the RGB channels untouched.
	 */
	public static int withAlpha(int argb, float factor) {
		if (factor >= 1.0F) {
			return argb;
		}
		float clamped = Math.max(0.0F, factor);
		int alpha = (int) (((argb >>> 24) & 0xFF) * clamped);
		return (alpha << 24) | (argb & 0x00FFFFFF);
	}
}
