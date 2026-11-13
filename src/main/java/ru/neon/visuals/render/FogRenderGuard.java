package ru.neon.visuals.render;

import com.mojang.blaze3d.systems.RenderSystem;
import ru.neon.visuals.config.VisualConfig;

/**
 * Temporarily disables the custom world fog while GUI player previews and
 * item models are rendered. Nesting is supported because item models can be
 * rendered from inside an inventory preview.
 */
public final class FogRenderGuard {
    private static int depth;

    private FogRenderGuard() {
    }

    public static void begin() {
        if (depth++ == 0 && VisualConfig.get().fogEnabled) {
            RenderSystem.setShaderFogStart(0.0f);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        }
    }

    public static void end() {
        if (depth <= 0) return;
        if (--depth == 0 && VisualConfig.get().fogEnabled) {
            FogController.applyConfiguredFog();
        }
    }

    public static boolean isSuppressingFog() {
        return depth > 0;
    }
}
