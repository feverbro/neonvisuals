package ru.neon.visuals.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.FogShape;
import ru.neon.visuals.config.VisualConfig;
import ru.neon.visuals.gui.VisualMenuScreen;

/** Applies the configured fog to terrain at the same distances as the camera render range. */
public final class FogController {
    private static float lastViewDistance = 256.0f;

    private FogController() {
    }

    /** True while one of the inventory-style client screens is open. */
    public static boolean shouldSuppressFogForUi() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.currentScreen instanceof InventoryScreen
                || client.currentScreen instanceof VisualMenuScreen;
    }

    /** Reuse the distance calculated for the current terrain pass when entities are rendered. */
    public static void applyConfiguredFog() {
        applyConfiguredFog(lastViewDistance);
    }

    public static void applyConfiguredFog(float viewDistance) {
        VisualConfig config = VisualConfig.get();
        if (!config.fogEnabled || FogRenderGuard.isSuppressingFog() || shouldSuppressFogForUi()) return;

        lastViewDistance = Math.max(4.0f, viewDistance);
        float maxDistance = lastViewDistance;

        // Begin blending close to the camera so fog is visible in the center,
        // not only on distant silhouettes at the horizon.
        float start = Math.min(Math.max(0.0f, config.fogStart), 2.0f);
        float end = Math.min(Math.max(start + 2.0f, config.fogEnd), maxDistance);

        int color = config.fogColor;
        RenderSystem.setShaderFogStart(start);
        RenderSystem.setShaderFogEnd(end);
        RenderSystem.setShaderFogColor(
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f,
                1.0f
        );
        RenderSystem.setShaderFogShape(config.fogSphere ? FogShape.SPHERE : FogShape.CYLINDER);
    }
}
