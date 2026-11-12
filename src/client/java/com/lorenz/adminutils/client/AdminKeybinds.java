package com.lorenz.adminutils.client;

import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.client.gui.AdminUtilsMenuScreen;
import com.lorenz.adminutils.client.gui.NavigationSection;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if <26.1 {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?} else {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?}

import net.minecraft.client.KeyMapping;

/**
 * Opens the AdminUtils menu.
 *
 * <p>Version-specific workaround: Fabric API renamed the key module in the 26.x line.
 * 1.21.11 ships {@code fabric-key-binding-api-v1} with
 * {@code KeyBindingHelper.registerKeyBinding}, while 26.1+ ships
 * {@code fabric-key-mapping-api-v1} with {@code KeyMappingHelper.registerKeyMapping}.
 * The vanilla {@link KeyMapping} type and its constructor are identical on both.
 */
public final class AdminKeybinds {
	private static KeyMapping openMenu;

	private AdminKeybinds() {
	}

	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(AdminUtils.id("main"));
		KeyMapping mapping = new KeyMapping("key.adminutils.open_menu", InputConstants.KEY_K, category);

		//? if <26.1 {
		/*openMenu = KeyBindingHelper.registerKeyBinding(mapping);
		*///?} else {
		openMenu = KeyMappingHelper.registerKeyMapping(mapping);
		//?}

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenu.consumeClick()) {
				AdminUtilsMenuScreen.open(NavigationSection.HOME);
			}
		});

		AdminUtils.LOGGER.info("Registered key binding '{}'", openMenu.getName());
	}
}
