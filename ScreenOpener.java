package de.keybindmap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Öffnet einen Screen. In den 26.x-Versionen wurde setScreen mehrfach
 * verschoben/umbenannt (Gui#setScreen, Minecraft#setScreenAndShow, ...).
 * Per Reflection probieren wir die bekannten Varianten durch, damit die Mod
 * bei kleinen Umbenennungen nicht gleich am Compiler scheitert.
 */
public final class ScreenOpener {

	private ScreenOpener() {
	}

	public static void open(Screen screen) {
		Minecraft mc = Minecraft.getInstance();

		if (invoke(mc, "setScreenAndShow", screen)) return;
		if (invoke(mc, "setScreen", screen)) return;

		try {
			Field guiField = mc.getClass().getField("gui");
			Object gui = guiField.get(mc);
			if (gui != null && invoke(gui, "setScreen", screen)) return;
		} catch (ReflectiveOperationException ignored) {
			// weiter zur Fehlermeldung
		}

		KeybindMapClient.LOGGER.error("Konnte den Screen nicht öffnen: keine passende setScreen-Methode gefunden");
	}

	private static boolean invoke(Object target, String name, Screen screen) {
		for (Method m : target.getClass().getMethods()) {
			if (!m.getName().equals(name) || m.getParameterCount() != 1) continue;
			if (!m.getParameterTypes()[0].isAssignableFrom(Screen.class)) continue;
			try {
				m.invoke(target, screen);
				return true;
			} catch (ReflectiveOperationException e) {
				KeybindMapClient.LOGGER.warn("setScreen über {} fehlgeschlagen", name, e);
			}
		}
		return false;
	}
}
