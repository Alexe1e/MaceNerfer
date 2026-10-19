package de.keybindmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Tastaturlayout (Vollformat, US-Belegung wie die G512) in Tasteneinheiten.
 * Die ids sind die Namen, die Minecraft in KeyMapping#saveString() verwendet,
 * z. B. "key.keyboard.g". Dadurch brauchen wir keine GLFW-Codes.
 */
public final class KeyboardLayout {

	public record Key(String id, String label, float x, float y, float w, float h) {
	}

	public static final float WIDTH_UNITS = 22.5f;
	public static final float TKL_WIDTH_UNITS = 18.25f;
	public static final float HEIGHT_UNITS = 7.75f;

	public static final List<Key> KEYS = build();

	private KeyboardLayout() {
	}

	private static List<Key> build() {
		List<Key> k = new ArrayList<>();

		// Reihe 0: Escape + Funktionstasten + Druck/Rollen/Pause
		add(k, "escape", "Esc", 0, 0, 1);
		for (int i = 0; i < 4; i++) add(k, "f" + (i + 1), "F" + (i + 1), 2 + i, 0, 1);
		for (int i = 4; i < 8; i++) add(k, "f" + (i + 1), "F" + (i + 1), 2.5f + i, 0, 1);
		for (int i = 8; i < 12; i++) add(k, "f" + (i + 1), "F" + (i + 1), 3f + i, 0, 1);
		add(k, "print.screen", "Prt", 15.25f, 0, 1);
		add(k, "scroll.lock", "Scr", 16.25f, 0, 1);
		add(k, "pause", "Pau", 17.25f, 0, 1);

		// Reihe 1: Zahlen
		float y = 1.5f;
		add(k, "grave.accent", "`", 0, y, 1);
		String[] nums = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
		for (int i = 0; i < nums.length; i++) add(k, nums[i], nums[i], 1 + i, y, 1);
		add(k, "minus", "-", 11, y, 1);
		add(k, "equal", "=", 12, y, 1);
		add(k, "backspace", "Bksp", 13, y, 2);
		add(k, "insert", "Ins", 15.25f, y, 1);
		add(k, "home", "Home", 16.25f, y, 1);
		add(k, "page.up", "PgUp", 17.25f, y, 1);
		add(k, "num.lock", "Num", 18.5f, y, 1);
		add(k, "keypad.divide", "/", 19.5f, y, 1);
		add(k, "keypad.multiply", "*", 20.5f, y, 1);
		add(k, "keypad.subtract", "-", 21.5f, y, 1);

		// Reihe 2: QWERTY
		y = 2.5f;
		add(k, "tab", "Tab", 0, y, 1.5f);
		String q = "qwertyuiop";
		for (int i = 0; i < q.length(); i++) letter(k, q.charAt(i), 1.5f + i, y);
		add(k, "left.bracket", "[", 11.5f, y, 1);
		add(k, "right.bracket", "]", 12.5f, y, 1);
		add(k, "backslash", "\\", 13.5f, y, 1.5f);
		add(k, "delete", "Del", 15.25f, y, 1);
		add(k, "end", "End", 16.25f, y, 1);
		add(k, "page.down", "PgDn", 17.25f, y, 1);
		add(k, "keypad.7", "7", 18.5f, y, 1);
		add(k, "keypad.8", "8", 19.5f, y, 1);
		add(k, "keypad.9", "9", 20.5f, y, 1);
		k.add(new Key("key.keyboard.keypad.add", "+", 21.5f, y, 1, 2));

		// Reihe 3: ASDF
		y = 3.5f;
		add(k, "caps.lock", "Caps", 0, y, 1.75f);
		String a = "asdfghjkl";
		for (int i = 0; i < a.length(); i++) letter(k, a.charAt(i), 1.75f + i, y);
		add(k, "semicolon", ";", 10.75f, y, 1);
		add(k, "apostrophe", "'", 11.75f, y, 1);
		add(k, "enter", "Enter", 12.75f, y, 2.25f);
		add(k, "keypad.4", "4", 18.5f, y, 1);
		add(k, "keypad.5", "5", 19.5f, y, 1);
		add(k, "keypad.6", "6", 20.5f, y, 1);

		// Reihe 4: YXCV
		y = 4.5f;
		add(k, "left.shift", "Shift", 0, y, 2.25f);
		String z = "zxcvbnm";
		for (int i = 0; i < z.length(); i++) letter(k, z.charAt(i), 2.25f + i, y);
		add(k, "comma", ",", 9.25f, y, 1);
		add(k, "period", ".", 10.25f, y, 1);
		add(k, "slash", "/", 11.25f, y, 1);
		add(k, "right.shift", "Shift", 12.25f, y, 2.75f);
		add(k, "up", "Up", 16.25f, y, 1);
		add(k, "keypad.1", "1", 18.5f, y, 1);
		add(k, "keypad.2", "2", 19.5f, y, 1);
		add(k, "keypad.3", "3", 20.5f, y, 1);
		k.add(new Key("key.keyboard.keypad.enter", "Ent", 21.5f, y, 1, 2));

		// Reihe 5: Leertaste usw.
		y = 5.5f;
		add(k, "left.control", "Ctrl", 0, y, 1.25f);
		add(k, "left.win", "Win", 1.25f, y, 1.25f);
		add(k, "left.alt", "Alt", 2.5f, y, 1.25f);
		add(k, "space", "Space", 3.75f, y, 6.25f);
		add(k, "right.alt", "Alt", 10, y, 1.25f);
		add(k, "right.win", "Win", 11.25f, y, 1.25f);
		add(k, "menu", "Menu", 12.5f, y, 1.25f);
		add(k, "right.control", "Ctrl", 13.75f, y, 1.25f);
		add(k, "left", "Left", 15.25f, y, 1);
		add(k, "down", "Down", 16.25f, y, 1);
		add(k, "right", "Right", 17.25f, y, 1);
		add(k, "keypad.0", "0", 18.5f, y, 2);
		add(k, "keypad.decimal", ".", 20.5f, y, 1);

		// Reihe 6: Maustasten
		y = 6.9f;
		k.add(new Key("key.mouse.left", "Maus L", 0, y, 2, 0.85f));
		k.add(new Key("key.mouse.middle", "Maus M", 2.1f, y, 2, 0.85f));
		k.add(new Key("key.mouse.right", "Maus R", 4.2f, y, 2, 0.85f));
		k.add(new Key("key.mouse.4", "Maus 4", 6.3f, y, 2, 0.85f));
		k.add(new Key("key.mouse.5", "Maus 5", 8.4f, y, 2, 0.85f));

		return List.copyOf(k);
	}

	private static void letter(List<Key> k, char c, float x, float y) {
		add(k, String.valueOf(c), String.valueOf(Character.toUpperCase(c)), x, y, 1);
	}

	private static void add(List<Key> k, String name, String label, float x, float y, float w) {
		k.add(new Key("key.keyboard." + name, label, x, y, w, 1));
	}
}
