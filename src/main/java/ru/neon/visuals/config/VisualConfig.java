package ru.neon.visuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent client-only settings. */
public final class VisualConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("neonvisuals.json");
    private static VisualConfig INSTANCE = new VisualConfig();

    // Render effects
    public boolean customCrosshair = true;
    public boolean hitMarker = true;
    public boolean hitParticles = true;
    public boolean jumpCircle = true;
    public boolean playerTrail = false;
    public boolean damagePulse = true;

    // HUD widgets
    public boolean fpsHud = true;
    public boolean coordinatesHud = true;
    public boolean pingHud = true;
    public boolean cpsHud = true;
    public boolean keystrokesHud = true;
    public boolean totemHud = true;
    public boolean armorHud = true;

    // Crosshair settings
    public int crosshairLength = 7;
    public int crosshairGap = 4;
    public int crosshairWidth = 2;
    public int crosshairOpacity = 240;
    public boolean crosshairDot = false;

    // Effects and theme
    public int pulseStrength = 105;
    public int particleAmount = 14;
    public int accentColor = 0xFF63D8FF;
    public int panelOpacity = 218;
    public boolean compactHud = false;

    private VisualConfig() {
    }

    public static VisualConfig get() {
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                VisualConfig loaded = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), VisualConfig.class);
                if (loaded != null) INSTANCE = loaded;
            }
            INSTANCE.sanitize();
        } catch (Exception exception) {
            System.err.println("[NeonVisuals] Could not load config: " + exception.getMessage());
            INSTANCE = new VisualConfig();
        }
    }

    public void save() {
        try {
            sanitize();
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("[NeonVisuals] Could not save config: " + exception.getMessage());
        }
    }

    private void sanitize() {
        crosshairLength = clamp(crosshairLength, 2, 18);
        crosshairGap = clamp(crosshairGap, 0, 12);
        crosshairWidth = clamp(crosshairWidth, 1, 6);
        crosshairOpacity = clamp(crosshairOpacity, 30, 255);
        pulseStrength = clamp(pulseStrength, 0, 200);
        particleAmount = clamp(particleAmount, 4, 40);
        panelOpacity = clamp(panelOpacity, 100, 255);
        accentColor |= 0xFF000000;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
