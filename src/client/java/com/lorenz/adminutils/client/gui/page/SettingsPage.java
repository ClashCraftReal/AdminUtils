package com.lorenz.adminutils.client.gui.page;

import com.lorenz.adminutils.client.config.AccentColor;
import com.lorenz.adminutils.client.config.ClientPreferences;
import com.lorenz.adminutils.client.config.ClientPreferencesIO;
import com.lorenz.adminutils.client.gui.AdminScreen;
import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.NavigationSection;
import com.lorenz.adminutils.client.gui.RenderContext;
import com.lorenz.adminutils.client.gui.widget.AdminButton;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Client-only UI preferences. Changes are written immediately and the screen is
 * rebuilt on the next tick so toggle labels, spacing and colours stay in sync.
 *
 * <p>The reset action is two-step: the first click only arms the confirmation, so a
 * stray click cannot wipe the user's settings.
 */
public final class SettingsPage extends AdminPage {
	private static final int FILE_LABEL_Y = 0;
	private static final int FILE_PATH_Y = 11;
	private static final int SECTION_LABEL_Y = 26;
	private static final int FIRST_TOGGLE_Y = 40;
	private static final int TOGGLE_STEP_NORMAL = 24;
	private static final int TOGGLE_STEP_COMPACT = 22;
	private static final int ACCENT_LABEL_GAP = 4;
	private static final int ACCENT_ROW_GAP = 12;
	private static final int ACCENT_BUTTON_WIDTH = 52;
	private static final int ACCENT_BUTTON_GAP = 4;
	private static final int ACCENT_BUTTON_SPACING = ACCENT_BUTTON_WIDTH + ACCENT_BUTTON_GAP;
	private static final int ACCENT_ROW_STEP = 24;
	private static final int RESET_GAP = 28;
	private static final int RESET_BUTTON_WIDTH = 180;
	private static final int CONFIRM_BUTTON_WIDTH = 88;
	private static final int CONFIRM_BUTTON_GAP = 4;
	private static final int CONFIRM_WARNING_Y = 22;
	private static final int FOOTER_GAP = 30;

	private boolean confirmReset;

	public SettingsPage() {
		super(NavigationSection.SETTINGS);
	}

	@Override
	public Component title() {
		return Component.literal("Settings");
	}

	@Override
	public Component subtitle() {
		return Component.literal("Client-side interface preferences");
	}

	@Override
	public void init(AdminScreen screen, int x, int y, int width, int height) {
		ClientPreferences preferences = ClientPreferencesIO.get();
		int step = toggleStep();

		screen.addButton(AdminButton.toggle(Component.literal("Permission details"),
			preferences.showPermissionDetails, x, y + FIRST_TOGGLE_Y, RESET_BUTTON_WIDTH, 20, pressed -> {
				preferences.showPermissionDetails = !preferences.showPermissionDetails;
				ClientPreferencesIO.save();
				screen.refresh();
			}));

		screen.addButton(AdminButton.toggle(Component.literal("Compact layout"),
			preferences.compactLayout, x, y + FIRST_TOGGLE_Y + step, RESET_BUTTON_WIDTH, 20, pressed -> {
				preferences.compactLayout = !preferences.compactLayout;
				ClientPreferencesIO.save();
				screen.refresh();
			}));

		screen.addButton(AdminButton.toggle(Component.literal("Animations"),
			preferences.animations, x, y + FIRST_TOGGLE_Y + step * 2, RESET_BUTTON_WIDTH, 20, pressed -> {
				preferences.animations = !preferences.animations;
				ClientPreferencesIO.save();
				screen.refresh();
			}));

		AccentColor[] accents = AccentColor.values();
		int available = Math.max(ACCENT_BUTTON_WIDTH, width);
		int columns = accentColumns(available);
		for (int index = 0; index < accents.length; index++) {
			AccentColor accent = accents[index];
			boolean selected = accent == AccentColor.byId(preferences.accentColor);
			int column = index % columns;
			int row = index / columns;
			screen.addButton(AdminButton.create(Component.literal(label(accent)),
				x + column * ACCENT_BUTTON_SPACING, accentRowY(y) + row * ACCENT_ROW_STEP,
				ACCENT_BUTTON_WIDTH, 20, !selected, pressed -> {
					preferences.accentColor = accent.id();
					ClientPreferencesIO.save();
					screen.refresh();
				}));
		}

		int resetY = resetY(y, available);
		if (this.confirmReset) {
			screen.addButton(AdminButton.create(Component.literal("Confirm reset"), x, resetY,
				CONFIRM_BUTTON_WIDTH, 20, true, pressed -> {
					ClientPreferencesIO.reset();
					this.confirmReset = false;
					screen.refresh();
					screen.notifications().success("Preferences reset to defaults");
				}));
			screen.addButton(AdminButton.create(Component.literal("Cancel"),
				x + CONFIRM_BUTTON_WIDTH + CONFIRM_BUTTON_GAP, resetY, CONFIRM_BUTTON_WIDTH, 20, true, pressed -> {
					this.confirmReset = false;
					screen.refresh();
				}));
		} else {
			screen.addButton(AdminButton.create(Component.literal("Reset to defaults"), x, resetY,
				RESET_BUTTON_WIDTH, 20, !preferences.isDefault(), pressed -> {
					this.confirmReset = true;
					screen.refresh();
				}));
		}
	}

	@Override
	public void render(RenderContext context, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
		ClientPreferences preferences = ClientPreferencesIO.get();
		AccentColor accent = AccentColor.byId(preferences.accentColor);

		context.text(font, Component.literal("Preferences file"), x, y + FILE_LABEL_Y, GuiTheme.TEXT_MUTED);
		context.text(font, Component.literal(ClientPreferencesIO.file().toString()), x, y + FILE_PATH_Y, GuiTheme.TEXT);

		context.text(font, Component.literal("Interface"), x, y + SECTION_LABEL_Y, GuiTheme.TEXT);
		context.text(font, Component.literal("Accent colour: " + label(accent)), x, accentLabelY(y), GuiTheme.TEXT);

		int resetY = resetY(y, width);
		int footerY = resetY + footerGap();

		context.text(font, Component.literal("Data version " + preferences.dataVersion), x, footerY, GuiTheme.TEXT_MUTED);
		context.text(font, Component.literal("These settings never leave this client."), x,
			footerY + GuiTheme.rowHeight(), GuiTheme.TEXT_MUTED);

		if (this.confirmReset) {
			context.text(font, Component.literal("Reset every preference to its default?"), x,
				resetY + CONFIRM_WARNING_Y, GuiTheme.WARNING);
		}
	}

	/**
	 * Distance from the reset button to the footer block. The confirmation warning sits
	 * between them, so the footer moves down far enough to clear it.
	 */
	private int footerGap() {
		return this.confirmReset ? CONFIRM_WARNING_Y + GuiTheme.rowHeight() + 6 : FOOTER_GAP;
	}

	private static int toggleStep() {
		return GuiTheme.compact() ? TOGGLE_STEP_COMPACT : TOGGLE_STEP_NORMAL;
	}

	private static int accentLabelY(int y) {
		return y + FIRST_TOGGLE_Y + toggleStep() * 3 + ACCENT_LABEL_GAP;
	}

	private static int accentRowY(int y) {
		return accentLabelY(y) + ACCENT_ROW_GAP;
	}

	/**
	 * How many accent buttons fit side by side in the available page width. Always at
	 * least one, so the selector stays usable at any GUI scale.
	 */
	private static int accentColumns(int availableWidth) {
		return Math.max(1, (availableWidth + ACCENT_BUTTON_GAP) / ACCENT_BUTTON_SPACING);
	}

	private static int accentRows(int availableWidth) {
		return (AccentColor.values().length + accentColumns(availableWidth) - 1) / accentColumns(availableWidth);
	}

	private static int resetY(int y, int availableWidth) {
		return accentRowY(y) + (accentRows(availableWidth) - 1) * ACCENT_ROW_STEP + RESET_GAP;
	}

	private static String label(AccentColor accent) {
		return switch (accent) {
			case BLUE -> "Blue";
			case TEAL -> "Teal";
			case VIOLET -> "Violet";
			case AMBER -> "Amber";
			case RED -> "Red";
		};
	}
}
