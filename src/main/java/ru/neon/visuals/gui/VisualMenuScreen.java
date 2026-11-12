package ru.neon.visuals.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.neon.visuals.config.VisualConfig;

import java.util.ArrayList;
import java.util.List;

public final class VisualMenuScreen extends Screen {
    private enum Tab { RENDER, HUD, STYLE }

    private Tab tab = Tab.RENDER;
    private String selected = "crosshair";
    private int scroll;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int sideW = 112;
    private int listX;
    private int listW;
    private int settingsX;
    private int settingsW;

    public VisualMenuScreen() {
        super(Text.literal("NeonVisuals"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        VisualConfig.get().save();
        if (client != null) client.setScreen(null);
    }

    @Override
    public void removed() {
        VisualConfig.get().save();
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA9050912);
        panelW = Math.min(650, width - 24);
        panelH = Math.min(370, height - 24);
        panelW = Math.max(420, panelW);
        panelH = Math.max(280, panelH);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sideW = 112;
        listX = panelX + sideW + 14;
        settingsW = Math.max(164, Math.min(220, panelW - sideW - 260));
        listW = panelW - sideW - settingsW - 42;
        settingsX = listX + listW + 12;

        drawPanel(context, panelX, panelY, panelW, panelH, 0xF20D1422);
        context.fill(panelX, panelY, panelX + panelW, panelY + 2, VisualConfig.get().accentColor);
        drawHeader(context);
        drawSidebar(context, mouseX, mouseY);
        drawModuleList(context, mouseX, mouseY);
        drawSettings(context, mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "RIGHT SHIFT  •  close menu", panelX + 16, panelY + panelH - 16, 0xFF6D7D92);
    }

    private void drawHeader(DrawContext context) {
        context.drawTextWithShadow(textRenderer, "NEON", panelX + 16, panelY + 14, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "VISUALS", panelX + 54, panelY + 14, VisualConfig.get().accentColor);
        context.drawTextWithShadow(textRenderer, "FABRIC 1.21  /  CLIENT VISUALS", panelX + 16, panelY + 30, 0xFF8190A6);
        context.fill(panelX + 14, panelY + 48, panelX + panelW - 14, panelY + 49, 0x334B5C74);
    }

    private void drawSidebar(DrawContext context, int mouseX, int mouseY) {
        String[] labels = {"RENDER", "HUD", "STYLE"};
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            int y = panelY + 65 + i * 34;
            boolean active = tab == tabs[i];
            boolean hover = inside(mouseX, mouseY, panelX + 10, y - 4, sideW - 14, 28);
            int background = active ? 0xFF1B2A3D : hover ? 0x6633455D : 0x00000000;
            context.fill(panelX + 10, y - 4, panelX + sideW - 4, y + 24, background);
            if (active) context.fill(panelX + 10, y - 4, panelX + 12, y + 24, VisualConfig.get().accentColor);
            context.drawTextWithShadow(textRenderer, labels[i], panelX + 21, y + 5,
                    active ? 0xFFFFFFFF : 0xFF8B9AB0);
        }

        context.fill(panelX + 12, panelY + panelH - 48, panelX + sideW - 4, panelY + panelH - 47, 0x334B5C74);
        context.drawTextWithShadow(textRenderer, "BUILD 1.0.0", panelX + 18, panelY + panelH - 36, 0xFF6E7F96);
        context.drawTextWithShadow(textRenderer, "SETTINGS SAVED", panelX + 18, panelY + panelH - 23, 0xFF65D6A4);
    }

    private void drawModuleList(DrawContext context, int mouseX, int mouseY) {
        String heading = switch (tab) {
            case RENDER -> "VISUAL EFFECTS";
            case HUD -> "HUD ELEMENTS";
            case STYLE -> "INTERFACE";
        };
        context.drawTextWithShadow(textRenderer, heading, listX, panelY + 62, 0xFF9BAAC0);
        List<Entry> entries = entriesForTab();
        int startY = panelY + 82;
        int rowH = 34;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int y = startY + i * rowH - scroll * rowH;
            if (y < panelY + 76 || y + 28 > panelY + panelH - 32) continue;
            boolean active = entry.enabled.getAsBoolean();
            boolean chosen = entry.key.equals(selected);
            boolean hover = inside(mouseX, mouseY, listX - 5, y, listW + 7, 29);
            int bg = chosen ? 0xFF1C2A3B : hover ? 0x66314155 : 0xAA121C2A;
            context.fill(listX - 5, y, listX + listW + 2, y + 29, bg);
            if (chosen) context.fill(listX - 5, y, listX - 3, y + 29, VisualConfig.get().accentColor);
            context.drawTextWithShadow(textRenderer, entry.label, listX + 5, y + 5, 0xFFF0F4FA);
            context.drawTextWithShadow(textRenderer, entry.description, listX + 5, y + 17, 0xFF8190A5);
            int toggleColor = active ? 0xFF59D69D : 0xFF526176;
            context.fill(listX + listW - 31, y + 8, listX + listW - 8, y + 21, toggleColor);
            context.fill(listX + listW - (active ? 14 : 29), y + 10, listX + listW - (active ? 10 : 25), y + 19, 0xFFFFFFFF);
        }
        if (entries.size() > 7) {
            context.drawTextWithShadow(textRenderer, "Scroll list  •  right click = settings", listX, panelY + panelH - 33, 0xFF708096);
        }
    }

    private void drawSettings(DrawContext context, int mouseX, int mouseY) {
        context.fill(settingsX - 7, panelY + 59, settingsX - 6, panelY + panelH - 28, 0x334B5C74);
        context.drawTextWithShadow(textRenderer, "QUICK SETTINGS", settingsX, panelY + 62, 0xFF9BAAC0);
        VisualConfig c = VisualConfig.get();
        int y = panelY + 88;

        switch (selected) {
            case "crosshair" -> {
                y = settingStepper(context, mouseX, mouseY, "Length", c.crosshairLength, 2, 18, settingsX, y, 1);
                y = settingStepper(context, mouseX, mouseY, "Gap", c.crosshairGap, 0, 12, settingsX, y, 2);
                y = settingStepper(context, mouseX, mouseY, "Thickness", c.crosshairWidth, 1, 6, settingsX, y, 3);
                y = settingStepper(context, mouseX, mouseY, "Opacity", c.crosshairOpacity, 30, 255, settingsX, y, 4);
                y = settingToggle(context, mouseX, mouseY, "Center dot", c.crosshairDot, settingsX, y, 5);
            }
            case "pulse" -> y = settingStepper(context, mouseX, mouseY, "Pulse strength", c.pulseStrength, 0, 200, settingsX, y, 6);
            case "particles" -> y = settingStepper(context, mouseX, mouseY, "Particle amount", c.particleAmount, 4, 40, settingsX, y, 7);
            case "style" -> {
                y = settingToggle(context, mouseX, mouseY, "Compact HUD", c.compactHud, settingsX, y, 8);
                y = settingStepper(context, mouseX, mouseY, "Panel opacity", c.panelOpacity, 100, 255, settingsX, y, 9);
                y = colorPicker(context, mouseX, mouseY, settingsX, y);
            }
            default -> {
                context.drawTextWithShadow(textRenderer, "Select a module", settingsX, y + 3, 0xFFE2E9F4);
                context.drawTextWithShadow(textRenderer, "Right-click a row to open", settingsX, y + 18, 0xFF8190A5);
                context.drawTextWithShadow(textRenderer, "its quick settings.", settingsX, y + 30, 0xFF8190A5);
                y += 54;
                y = colorPicker(context, mouseX, mouseY, settingsX, y);
            }
        }

        context.drawTextWithShadow(textRenderer, "Changes save automatically", settingsX, panelY + panelH - 34, 0xFF65D6A4);
    }

    private int settingStepper(DrawContext context, int mouseX, int mouseY, String label, int value,
                               int min, int max, int x, int y, int settingId) {
        context.drawTextWithShadow(textRenderer, label, x, y + 3, 0xFFE0E7F0);
        int bx = x + settingsW - 69;
        drawButton(context, bx, y - 1, 20, 18, "−", inside(mouseX, mouseY, bx, y - 1, 20, 18));
        context.drawTextWithShadow(textRenderer, Integer.toString(value), bx + 25, y + 3, 0xFFFFFFFF);
        drawButton(context, bx + 47, y - 1, 20, 18, "+", inside(mouseX, mouseY, bx + 47, y - 1, 20, 18));
        return y + 29;
    }

    private int settingToggle(DrawContext context, int mouseX, int mouseY, String label, boolean value,
                              int x, int y, int settingId) {
        context.drawTextWithShadow(textRenderer, label, x, y + 3, 0xFFE0E7F0);
        int bx = x + settingsW - 48;
        drawButton(context, bx, y - 1, 44, 18, value ? "ON" : "OFF", inside(mouseX, mouseY, bx, y - 1, 44, 18));
        return y + 29;
    }

    private int colorPicker(DrawContext context, int mouseX, int mouseY, int x, int y) {
        context.drawTextWithShadow(textRenderer, "Accent color", x, y + 3, 0xFFE0E7F0);
        int[] colors = {0xFF63D8FF, 0xFFB78CFF, 0xFFFF78B7, 0xFF70E0A1, 0xFFFFC857, 0xFFFF7777, 0xFFFFFFFF};
        int startX = x;
        for (int i = 0; i < colors.length; i++) {
            int bx = startX + i * 22;
            context.fill(bx, y + 21, bx + 17, y + 38, colors[i]);
            if (VisualConfig.get().accentColor == colors[i]) {
                context.fill(bx - 1, y + 20, bx + 18, y + 21, 0xFFFFFFFF);
                context.fill(bx - 1, y + 38, bx + 18, y + 39, 0xFFFFFFFF);
            }
        }
        return y + 51;
    }

    private void drawButton(DrawContext context, int x, int y, int w, int h, String label, boolean hover) {
        context.fill(x, y, x + w, y + h, hover ? 0xFF30445D : 0xFF1A2636);
        int textW = textRenderer.getWidth(label);
        context.drawTextWithShadow(textRenderer, label, x + (w - textW) / 2, y + 5, 0xFFE8EFF8);
    }

    private void drawPanel(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + h, color);
        context.fill(x, y, x + w, y + 1, 0x553F5874);
        context.fill(x, y + h - 1, x + w, y + h, 0x773F5874);
        context.fill(x, y, x + 1, y + h, 0x553F5874);
        context.fill(x + w - 1, y, x + w, y + h, 0x553F5874);
    }

    private List<Entry> entriesForTab() {
        List<Entry> entries = new ArrayList<>();
        VisualConfig c = VisualConfig.get();
        switch (tab) {
            case RENDER -> {
                entries.add(new Entry("crosshair", "Custom Crosshair", "Adjustable center reticle", () -> c.customCrosshair));
                entries.add(new Entry("hitmarker", "Hit Marker", "Short impact feedback", () -> c.hitMarker));
                entries.add(new Entry("particles", "Hit Particles", "Local particle burst on click", () -> c.hitParticles));
                entries.add(new Entry("jump", "Jump Circle", "Particle ring when jumping", () -> c.jumpCircle));
                entries.add(new Entry("trail", "Player Trail", "Light trail while airborne", () -> c.playerTrail));
                entries.add(new Entry("pulse", "Damage Pulse", "Subtle screen tint on damage", () -> c.damagePulse));
            }
            case HUD -> {
                entries.add(new Entry("fps", "FPS Counter", "Current client frame rate", () -> c.fpsHud));
                entries.add(new Entry("coords", "Coordinates", "XYZ position display", () -> c.coordinatesHud));
                entries.add(new Entry("ping", "Ping", "Network latency", () -> c.pingHud));
                entries.add(new Entry("cps", "CPS Counter", "Left clicks in the last second", () -> c.cpsHud));
                entries.add(new Entry("keys", "Keystrokes", "WASD and jump key display", () -> c.keystrokesHud));
                entries.add(new Entry("totems", "Totem Counter", "Totems in inventory", () -> c.totemHud));
                entries.add(new Entry("armor", "Armor HUD", "Durability of worn armor", () -> c.armorHud));
            }
            case STYLE -> {
                entries.add(new Entry("style", "HUD Theme", "Compact layout and colors", () -> true));
            }
        }
        return entries;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        VisualConfig c = VisualConfig.get();
        // Sidebar tabs
        for (int i = 0; i < Tab.values().length; i++) {
            int y = panelY + 61 + i * 34;
            if (inside(mouseX, mouseY, panelX + 10, y, sideW - 14, 28)) {
                tab = Tab.values()[i];
                scroll = 0;
                if (tab == Tab.STYLE) selected = "style";
                else selected = entriesForTab().getFirst().key;
                return true;
            }
        }

        List<Entry> entries = entriesForTab();
        int startY = panelY + 82;
        int rowH = 34;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int y = startY + i * rowH - scroll * rowH;
            if (!inside(mouseX, mouseY, listX - 5, y, listW + 7, 29)) continue;
            selected = entry.key;
            if (button == 1) return true;
            setModule(entry.key, !entry.enabled.getAsBoolean());
            c.save();
            return true;
        }

        int x = settingsX;
        int y = panelY + 88;
        int direction;
        switch (selected) {
            case "crosshair" -> {
                direction = stepperHit(mouseX, mouseY, x, y);
                if (direction != 0) c.crosshairLength = clamp(c.crosshairLength + direction, 2, 18);
                else if ((direction = stepperHit(mouseX, mouseY, x, y + 29)) != 0) c.crosshairGap = clamp(c.crosshairGap + direction, 0, 12);
                else if ((direction = stepperHit(mouseX, mouseY, x, y + 58)) != 0) c.crosshairWidth = clamp(c.crosshairWidth + direction, 1, 6);
                else if ((direction = stepperHit(mouseX, mouseY, x, y + 87)) != 0) c.crosshairOpacity = clamp(c.crosshairOpacity + direction * 10, 30, 255);
                else if (inside(mouseX, mouseY, x + settingsW - 48, y + 115, 44, 18)) c.crosshairDot = !c.crosshairDot;
                else return super.mouseClicked(mouseX, mouseY, button);
            }
            case "pulse" -> {
                direction = stepperHit(mouseX, mouseY, x, y);
                if (direction == 0) return super.mouseClicked(mouseX, mouseY, button);
                c.pulseStrength = clamp(c.pulseStrength + direction * 10, 0, 200);
            }
            case "particles" -> {
                direction = stepperHit(mouseX, mouseY, x, y);
                if (direction == 0) return super.mouseClicked(mouseX, mouseY, button);
                c.particleAmount = clamp(c.particleAmount + direction * 2, 4, 40);
            }
            case "style" -> {
                if (inside(mouseX, mouseY, x + settingsW - 48, y - 1, 44, 18)) c.compactHud = !c.compactHud;
                else if ((direction = stepperHit(mouseX, mouseY, x, y + 29)) != 0) c.panelOpacity = clamp(c.panelOpacity + direction * 10, 100, 255);
                else if (inside(mouseX, mouseY, x, y + 79, settingsW, 18)) chooseColor(mouseX, x);
                else return super.mouseClicked(mouseX, mouseY, button);
            }
            default -> {
                if (inside(mouseX, mouseY, x, y + 75, settingsW, 18)) chooseColor(mouseX, x);
                else return super.mouseClicked(mouseX, mouseY, button);
            }
        }
        c.save();
        return true;
    }

    /** Returns -1 for the decrement button, +1 for increment, or 0 for no hit. */
    private int stepperHit(double mouseX, double mouseY, int x, int y) {
        int bx = x + settingsW - 69;
        if (inside(mouseX, mouseY, bx, y - 1, 20, 18)) return -1;
        if (inside(mouseX, mouseY, bx + 47, y - 1, 20, 18)) return 1;
        return 0;
    }

    private void chooseColor(double mouseX, int x) {
        int[] colors = {0xFF63D8FF, 0xFFB78CFF, 0xFFFF78B7, 0xFF70E0A1, 0xFFFFC857, 0xFFFF7777, 0xFFFFFFFF};
        int index = (int) ((mouseX - x) / 22.0);
        if (index >= 0 && index < colors.length) VisualConfig.get().accentColor = colors[index];
    }

    private void setModule(String key, boolean value) {
        VisualConfig c = VisualConfig.get();
        switch (key) {
            case "crosshair" -> c.customCrosshair = value;
            case "hitmarker" -> c.hitMarker = value;
            case "particles" -> c.hitParticles = value;
            case "jump" -> c.jumpCircle = value;
            case "trail" -> c.playerTrail = value;
            case "pulse" -> c.damagePulse = value;
            case "fps" -> c.fpsHud = value;
            case "coords" -> c.coordinatesHud = value;
            case "ping" -> c.pingHud = value;
            case "cps" -> c.cpsHud = value;
            case "keys" -> c.keystrokesHud = value;
            case "totems" -> c.totemHud = value;
            case "armor" -> c.armorHud = value;
            default -> { }
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private record Entry(String key, String label, String description, java.util.function.BooleanSupplier enabled) {
    }
}
