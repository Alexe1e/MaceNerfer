package de.keybindmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Tastatur-Karte: links die Tastatur, rechts ein Popup.
 * Ohne Auswahl zeigt das Popup alle Konflikte (Tasten mit mehreren Keybinds),
 * mit Auswahl die Keybinds der gewaehlten Taste.
 */
public class KeybindMapScreen extends Screen {

	// ---- Farben ------------------------------------------------------------
	private static final int BG_CARD = 0xD0121620;
	private static final int CARD_BORDER = 0xFF2B3140;

	private static final int FREE_FACE = 0xFF2A2F3B;
	private static final int FREE_EDGE = 0xFF1A1E27;
	private static final int USED_FACE = 0xFF3B82F6;
	private static final int USED_EDGE = 0xFF1E4FB8;
	private static final int CONF_FACE = 0xFFF08A24;
	private static final int CONF_EDGE = 0xFFB4560C;

	private static final int TEXT = 0xFFE8ECF4;
	private static final int MUTED = 0xFF8E98AB;
	private static final int FREE_TEXT = 0xFF778196;
	private static final int ORANGE = 0xFFFFA64D;
	private static final int BLUE = 0xFF6BA3FF;
	private static final int WHITE = 0xFFFFFFFF;

	private static final int STATE_FREE = 0;
	private static final int STATE_USED = 1;
	private static final int STATE_CONFLICT = 2;

	private record Hit(int x1, int y1, int x2, int y2, String id) {
		boolean contains(double x, double y) {
			return x >= x1 && x < x2 && y >= y1 && y < y2;
		}
	}

	private final Screen parent;

	/** Tasten-id (z. B. "key.keyboard.g") -> alle Keybinds darauf. */
	private final Map<String, List<KeyMapping>> usage = new HashMap<>();
	private final List<KeyboardLayout.Key> visible = new ArrayList<>();
	private final List<Hit> hits = new ArrayList<>();

	/** null = Uebersicht, sonst Tasten-id. */
	private String selected;

	private int statUsed, statFree, statConflict;

	// Geometrie
	private float unit;
	private int keysX, keysY;
	private int kbCardX, kbCardY, kbCardW, kbCardH;
	private int panelX, panelY, panelW, panelH;

	public KeybindMapScreen(Screen parent) {
		super(Component.translatable("keybindmap.title"));
		this.parent = parent;
	}

	// ------------------------------------------------------------------ init

	@Override
	protected void init() {
		collectUsage();

		final int margin = 8;
		final int headerH = 32;
		final int pad = 6;

		panelW = Math.max(132, Math.min(190, Math.round(this.width * 0.30f)));
		panelX = this.width - margin - panelW;
		panelY = headerH;
		panelH = this.height - headerH - 34;

		int kbAvailW = panelX - 2 * margin;
		int innerW = kbAvailW - 2 * pad;
		int availH = this.height - headerH - 34 - 2 * pad - 14;

		boolean numpad = innerW / KeyboardLayout.WIDTH_UNITS >= 17f;
		float layoutW = numpad ? KeyboardLayout.WIDTH_UNITS : KeyboardLayout.TKL_WIDTH_UNITS;

		unit = Math.min(Math.min(innerW / layoutW, availH / KeyboardLayout.HEIGHT_UNITS), 26f);
		unit = Math.max(unit, 8f);

		kbCardW = Math.round(layoutW * unit) + 2 * pad;
		kbCardH = Math.round(KeyboardLayout.HEIGHT_UNITS * unit) + 2 * pad;
		kbCardX = margin + Math.max(0, (kbAvailW - kbCardW) / 2);
		kbCardY = headerH;
		keysX = kbCardX + pad;
		keysY = kbCardY + pad;

		visible.clear();
		for (KeyboardLayout.Key key : KeyboardLayout.KEYS) {
			if (key.x() + key.w() <= layoutW + 0.01f) visible.add(key);
		}

		statUsed = 0;
		statFree = 0;
		statConflict = 0;
		for (KeyboardLayout.Key key : visible) {
			int c = count(key.id());
			if (c == 0) statFree++;
			else {
				statUsed++;
				if (c > 1) statConflict++;
			}
		}

		this.addRenderableWidget(Button.builder(
				Component.translatable("keybindmap.done"),
				btn -> this.onClose()
		).bounds(panelX, this.height - 28, panelW, 20).build());
	}

	private void collectUsage() {
		usage.clear();
		for (KeyMapping mapping : this.minecraft.options.keyMappings) {
			if (mapping.isUnbound()) continue;
			usage.computeIfAbsent(mapping.saveString(), id -> new ArrayList<>()).add(mapping);
		}
	}

	private int count(String id) {
		return usage.getOrDefault(id, List.of()).size();
	}

	private static int stateOf(int count) {
		return count == 0 ? STATE_FREE : (count == 1 ? STATE_USED : STATE_CONFLICT);
	}

	@Override
	public void onClose() {
		ScreenOpener.open(parent);
	}

	// ----------------------------------------------------------------- Input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;

		double mx = event.x();
		double my = event.y();

		for (Hit hit : hits) {
			if (hit.contains(mx, my)) {
				selected = hit.id().isEmpty() ? null : hit.id();
				return true;
			}
		}

		KeyboardLayout.Key key = keyAt(mx, my);
		if (key != null) {
			selected = key.id().equals(selected) ? null : key.id();
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_ESCAPE) {
			if (selected != null) {
				selected = null;
				return true;
			}
			return super.keyPressed(event);
		}
		String id = InputConstants.Type.KEYBOARD.getOrCreate(event.key()).getName();
		for (KeyboardLayout.Key key : visible) {
			if (key.id().equals(id)) {
				selected = id;
				return true;
			}
		}
		return super.keyPressed(event);
	}

	private int[] rect(KeyboardLayout.Key k) {
		int x1 = keysX + Math.round(k.x() * unit) + 1;
		int y1 = keysY + Math.round(k.y() * unit) + 1;
		int x2 = keysX + Math.round((k.x() + k.w()) * unit) - 1;
		int y2 = keysY + Math.round((k.y() + k.h()) * unit) - 1;
		return new int[]{x1, y1, x2, y2};
	}

	private KeyboardLayout.Key keyAt(double mx, double my) {
		for (KeyboardLayout.Key key : visible) {
			int[] r = rect(key);
			if (mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3]) return key;
		}
		return null;
	}

	private KeyboardLayout.Key find(String id) {
		for (KeyboardLayout.Key key : visible) {
			if (key.id().equals(id)) return key;
		}
		return null;
	}

	// ------------------------------------------------------------- Rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);

		drawHeader(g);
		drawKeyboard(g, mouseX, mouseY);
		drawPanel(g, mouseX, mouseY);
		drawTooltip(g, mouseX, mouseY);
	}

	private void drawHeader(GuiGraphicsExtractor g) {
		scaledText(g, this.title.getString(), 8, 8, 1.5f, WHITE, true);

		int right = kbCardX + kbCardW;
		String conflict = Component.translatable("keybindmap.stat.conflict", statConflict).getString();
		String free = Component.translatable("keybindmap.stat.free", statFree).getString();
		String used = Component.translatable("keybindmap.stat.used", statUsed).getString();

		int y = 12;
		right = chip(g, right, y, CONF_FACE, conflict);
		right = chip(g, right - 10, y, FREE_FACE, free);
		chip(g, right - 10, y, USED_FACE, used);
	}

	/** Zeichnet ein Chip rechtsbuendig bei x und gibt dessen linke Kante zurueck. */
	private int chip(GuiGraphicsExtractor g, int rightX, int y, int color, String label) {
		int w = this.font.width(label);
		int x = rightX - w;
		g.text(this.font, label, x, y, TEXT, false);
		rr(g, x - 11, y + 1, x - 4, y + 8, color, 1);
		return x - 11;
	}

	private void drawKeyboard(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		card(g, kbCardX, kbCardY, kbCardX + kbCardW, kbCardY + kbCardH);

		KeyboardLayout.Key hovered = keyAt(mouseX, mouseY);
		int depth = unit >= 14f ? 2 : 1;

		for (KeyboardLayout.Key key : visible) {
			int c = count(key.id());
			int state = stateOf(c);
			int[] r = rect(key);

			boolean isSelected = key.id().equals(selected);
			boolean isHover = key == hovered;

			if (isSelected) {
				border(g, r[0] - 2, r[1] - 2, r[2] + 2, r[3] + 2, 0x55FFFFFF);
				border(g, r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, WHITE);
			}

			cap(g, r[0], r[1], r[2], r[3], state, depth, isHover || isSelected);

			int faceBottom = r[3] - depth;
			int cx = (r[0] + r[2]) / 2;
			int cy = (r[1] + faceBottom) / 2;
			int textColor = state == STATE_FREE ? FREE_TEXT : WHITE;
			centeredText(g, key.label(), cx, cy, r[2] - r[0] - 4, 1f, textColor, state != STATE_FREE);

			// kleine Zaehler-Plakette bei Konflikten
			if (c > 1 && unit >= 15f) {
				String n = String.valueOf(c);
				int bw = 8;
				rr(g, r[2] - bw - 1, r[1] + 1, r[2] - 1, r[1] + 9, 0xFF161922, 1);
				centeredText(g, n, r[2] - 1 - bw / 2, r[1] + 5, bw - 1, 0.7f, WHITE, false);
			}
		}

		int tipY = kbCardY + kbCardH + 5;
		g.text(this.font, Component.translatable("keybindmap.hint").getString(), kbCardX + 2, tipY, MUTED, false);
	}

	private void cap(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int state, int depth, boolean lit) {
		int face = state == STATE_FREE ? FREE_FACE : (state == STATE_USED ? USED_FACE : CONF_FACE);
		int edge = state == STATE_FREE ? FREE_EDGE : (state == STATE_USED ? USED_EDGE : CONF_EDGE);
		rr(g, x1, y1, x2, y2, edge, 1);
		rr(g, x1, y1, x2, y2 - depth, face, 1);
		g.fill(x1 + 2, y1 + 1, x2 - 2, y1 + 2, lit ? 0x70FFFFFF : 0x30FFFFFF);
	}

	// ---------------------------------------------------------------- Popup

	private void drawPanel(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		hits.clear();
		card(g, panelX, panelY, panelX + panelW, panelY + panelH);

		int x = panelX + 8;
		int y = panelY + 8;
		int w = panelW - 16;
		int bottom = panelY + panelH - 6;

		if (selected == null) {
			drawOverview(g, mouseX, mouseY, x, y, w, bottom);
		} else {
			drawDetail(g, mouseX, mouseY, x, y, w, bottom);
		}
	}

	private void drawOverview(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int y, int w, int bottom) {
		scaledText(g, Component.translatable("keybindmap.overview.title").getString(), x, y, 1.25f, WHITE, true);
		String count = String.valueOf(statConflict);
		int cw = this.font.width(count) + 8;
		rr(g, x + w - cw, y - 1, x + w, y + 10, statConflict > 0 ? CONF_FACE : USED_FACE, 1);
		centeredText(g, count, x + w - cw / 2, y + 5, cw - 2, 1f, WHITE, true);
		y += 18;

		List<KeyboardLayout.Key> conflicts = new ArrayList<>();
		for (KeyboardLayout.Key key : visible) {
			if (count(key.id()) > 1) conflicts.add(key);
		}

		String hint = Component.translatable(conflicts.isEmpty()
				? "keybindmap.overview.none" : "keybindmap.overview.hint").getString();
		for (String line : wrap(hint, w)) {
			g.text(this.font, line, x, y, MUTED, false);
			y += this.font.lineHeight + 1;
		}
		y += 5;

		final int rowH = 27;
		int maxRows = Math.max(0, (bottom - y) / rowH);
		for (int i = 0; i < conflicts.size(); i++) {
			if (i == maxRows - 1 && conflicts.size() > maxRows) {
				String more = Component.translatable("keybindmap.more", conflicts.size() - i).getString();
				g.text(this.font, more, x, y + 4, MUTED, false);
				break;
			}
			KeyboardLayout.Key key = conflicts.get(i);
			List<KeyMapping> list = usage.get(key.id());

			Hit hit = new Hit(x - 4, y, x + w + 4, y + rowH - 2, key.id());
			hits.add(hit);
			if (hit.contains(mouseX, mouseY)) {
				rr(g, hit.x1(), hit.y1(), hit.x2(), hit.y2(), 0x28FFFFFF, 1);
			}

			int capW = Math.max(20, this.font.width(key.label()) + 8);
			capW = Math.min(capW, 34);
			cap(g, x, y + 2, x + capW, y + 21, STATE_CONFLICT, 2, false);
			centeredText(g, key.label(), x + capW / 2, y + 10, capW - 3, 1f, WHITE, true);

			int tx = x + capW + 6;
			int tw = w - capW - 6;
			String head = Component.translatable("keybindmap.binds", list.size()).getString();
			g.text(this.font, head, tx, y + 2, ORANGE, false);

			StringBuilder names = new StringBuilder();
			for (int j = 0; j < list.size(); j++) {
				if (j > 0) names.append(", ");
				names.append(Component.translatable(list.get(j).getName()).getString());
			}
			g.text(this.font, fit(names.toString(), tw), tx, y + 12, MUTED, false);

			y += rowH;
		}
	}

	private void drawDetail(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int y, int w, int bottom) {
		KeyboardLayout.Key key = find(selected);
		if (key == null) {
			selected = null;
			return;
		}
		List<KeyMapping> list = usage.getOrDefault(key.id(), List.of());
		int state = stateOf(list.size());

		// Zurueck-Link
		String back = Component.translatable("keybindmap.back").getString();
		Hit backHit = new Hit(x - 4, y - 3, x + this.font.width(back) + 4, y + 11, "");
		hits.add(backHit);
		g.text(this.font, back, x, y, backHit.contains(mouseX, mouseY) ? WHITE : MUTED, false);
		y += 18;

		// grosse Taste + Status
		int capW = Math.max(34, Math.min(60, this.font.width(key.label()) * 2 + 14));
		cap(g, x, y, x + capW, y + 30, state, 3, false);
		centeredText(g, key.label(), x + capW / 2, y + 13, capW - 6, 1.6f, state == STATE_FREE ? FREE_TEXT : WHITE, state != STATE_FREE);

		int tx = x + capW + 8;
		String stateKey = state == STATE_FREE ? "keybindmap.state.free"
				: (state == STATE_USED ? "keybindmap.state.used" : "keybindmap.state.conflict");
		int stateColor = state == STATE_FREE ? MUTED : (state == STATE_USED ? BLUE : ORANGE);
		scaledText(g, Component.translatable(stateKey).getString(), tx, y + 2, 1.2f, stateColor, true);
		if (!list.isEmpty()) {
			String binds = list.size() == 1
					? Component.translatable("keybindmap.bind.one").getString()
					: Component.translatable("keybindmap.binds", list.size()).getString();
			g.text(this.font, binds, tx, y + 17, MUTED, false);
		}
		y += 40;

		// Hinweistext
		String info = null;
		if (state == STATE_FREE) info = Component.translatable("keybindmap.detail.free").getString();
		else if (state == STATE_CONFLICT) info = Component.translatable("keybindmap.detail.conflict").getString();
		if (info != null) {
			for (String line : wrap(info, w)) {
				g.text(this.font, line, x, y, state == STATE_CONFLICT ? ORANGE : MUTED, false);
				y += this.font.lineHeight + 1;
			}
			y += 5;
		}

		// Liste der Keybinds
		int lineH = this.font.lineHeight + 5;
		int maxLines = Math.max(0, (bottom - y) / lineH);
		for (int i = 0; i < list.size(); i++) {
			if (i == maxLines - 1 && list.size() > maxLines) {
				String more = Component.translatable("keybindmap.more", list.size() - i).getString();
				g.text(this.font, more, x, y, MUTED, false);
				break;
			}
			int dot = state == STATE_CONFLICT ? CONF_FACE : USED_FACE;
			rr(g, x, y + 2, x + 5, y + 7, dot, 1);
			String name = Component.translatable(list.get(i).getName()).getString();
			g.text(this.font, fit(name, w - 11), x + 10, y, TEXT, false);
			y += lineH;
		}
	}

	// -------------------------------------------------------------- Tooltip

	private void drawTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		KeyboardLayout.Key key = keyAt(mouseX, mouseY);
		if (key == null) return;

		List<KeyMapping> list = usage.getOrDefault(key.id(), List.of());
		int state = stateOf(list.size());

		List<String> lines = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		String stateKey = state == STATE_FREE ? "keybindmap.state.free"
				: (state == STATE_USED ? "keybindmap.state.used" : "keybindmap.state.conflict");
		lines.add(key.label() + "  -  " + Component.translatable(stateKey).getString());
		colors.add(state == STATE_FREE ? MUTED : (state == STATE_USED ? BLUE : ORANGE));
		int shown = Math.min(list.size(), 5);
		for (int i = 0; i < shown; i++) {
			lines.add(Component.translatable(list.get(i).getName()).getString());
			colors.add(TEXT);
		}
		if (list.size() > shown) {
			lines.add(Component.translatable("keybindmap.more", list.size() - shown).getString());
			colors.add(MUTED);
		}

		int w = 0;
		for (String s : lines) w = Math.max(w, this.font.width(s));
		w += 12;
		int h = lines.size() * (this.font.lineHeight + 1) + 8;

		int x = mouseX + 12;
		int y = mouseY + 12;
		if (x + w > this.width - 4) x = mouseX - w - 8;
		if (y + h > this.height - 4) y = this.height - 4 - h;

		rr(g, x, y, x + w, y + h, 0xFF3A4152, 2);
		rr(g, x + 1, y + 1, x + w - 1, y + h - 1, 0xF2101218, 2);
		int ty = y + 5;
		for (int i = 0; i < lines.size(); i++) {
			g.text(this.font, lines.get(i), x + 6, ty, colors.get(i), false);
			ty += this.font.lineHeight + 1;
		}
	}

	// -------------------------------------------------------------- Helfer

	private void card(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
		rr(g, x1, y1, x2, y2, CARD_BORDER, 2);
		rr(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, BG_CARD, 2);
	}

	/** Abgerundetes Rechteck ohne ueberlappende Fuellungen (wichtig bei Transparenz). */
	private static void rr(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color, int r) {
		if (x2 - x1 < 2 * r + 1 || y2 - y1 < 2 * r + 1) {
			g.fill(x1, y1, x2, y2, color);
			return;
		}
		if (r <= 1) {
			g.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
			g.fill(x1, y1 + 1, x2, y2 - 1, color);
			g.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
			return;
		}
		g.fill(x1 + 2, y1, x2 - 2, y1 + 1, color);
		g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, color);
		g.fill(x1, y1 + 2, x2, y2 - 2, color);
		g.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, color);
		g.fill(x1 + 2, y2 - 1, x2 - 2, y2, color);
	}

	private static void border(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
		g.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
		g.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
		g.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
	}

	private void scaledText(GuiGraphicsExtractor g, String s, float x, float y, float scale, int color, boolean shadow) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(this.font, s, 0, 0, color, shadow);
		g.pose().popMatrix();
	}

	/** Zentriert Text; verkleinert ihn automatisch, falls er nicht in maxW passt. */
	private void centeredText(GuiGraphicsExtractor g, String s, int cx, int cy, int maxW, float maxScale, int color, boolean shadow) {
		int w = this.font.width(s);
		if (w <= 0) return;
		float scale = Math.min(maxScale, maxW / (float) w);
		scale = Math.max(scale, 0.45f);
		float x = cx - w * scale / 2f;
		float y = cy - 3.5f * scale;
		scaledText(g, s, x, y, scale, color, shadow);
	}

	private String fit(String s, int maxW) {
		if (this.font.width(s) <= maxW) return s;
		String ellipsis = "...";
		int ew = this.font.width(ellipsis);
		StringBuilder b = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (this.font.width(b.toString() + c) + ew > maxW) break;
			b.append(c);
		}
		return b + ellipsis;
	}

	private List<String> wrap(String s, int maxW) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : s.split(" ")) {
			String test = line.isEmpty() ? word : line + " " + word;
			if (this.font.width(test) > maxW && !line.isEmpty()) {
				out.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(test);
			}
		}
		if (!line.isEmpty()) out.add(line.toString());
		return out;
	}
}
