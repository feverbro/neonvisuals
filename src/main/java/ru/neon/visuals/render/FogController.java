package ru.neon.visuals.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.FogShape;
import ru.neon.visuals.config.VisualConfig;

/** Applies custom fog uniforms while keeping the fog inside the actual view distance. */
public final class FogController {
    private static float lastViewDistance = 256.0f;

    private FogController() {
    }

    /** Reuse the view distance calculated for the current world render pass. */
    public static void applyConfiguredFog() {
        applyConfiguredFog(lastViewDistance);
    }

    public static void applyConfiguredFog(float viewDistance) {
        VisualConfig config = VisualConfig.get();
        if (!config.fogEnabled) return;

        lastViewDistance = Math.max(4.0f, viewDistance);

        int color = config.fogColor;

        // The configured fog must not end beyond the camera's actual render range.
        // Clamp the start as well, so short view distances still reach full fog color.
        float maxDistance = lastViewDistance;
        float start = Math.min(Math.max(0.0f, config.fogStart), maxDistance - 2.0f);
        float end = Math.min(Math.max(start + 2.0f, config.fogEnd), maxDistance);

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
