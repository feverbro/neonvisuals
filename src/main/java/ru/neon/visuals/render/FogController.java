package ru.neon.visuals.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.FogShape;
import ru.neon.visuals.config.VisualConfig;

/** Re-applies the custom fog uniforms before terrain and entity render passes. */
public final class FogController {
    private FogController() {
    }

    public static void applyConfiguredFog() {
        VisualConfig config = VisualConfig.get();
        if (!config.fogEnabled) return;

        int color = config.fogColor;
        // Keep the fog range in world-block units and let it reach the configured horizon.
        float start = Math.max(0.0f, config.fogStart);
        float end = Math.max(start + 2.0f, config.fogEnd);
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
