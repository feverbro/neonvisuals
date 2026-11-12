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
        RenderSystem.setShaderFogStart(config.fogStart);
        RenderSystem.setShaderFogEnd(Math.max(config.fogStart + 2, config.fogEnd));
        RenderSystem.setShaderFogColor(
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f,
                1.0f
        );
        RenderSystem.setShaderFogShape(config.fogSphere ? FogShape.SPHERE : FogShape.CYLINDER);
    }
}
