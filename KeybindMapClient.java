package de.keybindmap;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindMapClient implements ClientModInitializer {

	public static final String MOD_ID = "keybindmap";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyMapping openKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

		openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.keybindmap.open",
				InputConstants.Type.KEYBOARD,
				InputConstants.KEY_F8,
				category
		));

		// Im Spiel: F8 öffnet die Karte
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				ScreenOpener.open(new KeybindMapScreen(null));
			}
		});

		// Im Menü "Steuerung > Tastenbelegung": zusätzlicher Button oben links
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof KeyBindsScreen) {
				Button button = Button.builder(
						Component.translatable("keybindmap.button"),
						btn -> ScreenOpener.open(new KeybindMapScreen(screen))
				).bounds(6, 6, 100, 20).build();
				Screens.getWidgets(screen).add(button);
			}
		});
	}
}
