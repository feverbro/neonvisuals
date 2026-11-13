package ru.neon.visuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.config.VisualConfig;
import ru.neon.visuals.render.FogController;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    /**
     * applyFogColor computes biome/sky colors internally, so override them
     * after its calculations rather than before them. Inventory-style screens
     * keep the original sky color and do not receive the custom fog tint.
     */
    @Inject(method = "applyFogColor", at = @At("TAIL"))
    private static void neonvisuals$setFogHorizonColor(CallbackInfo ci) {
        VisualConfig config = VisualConfig.get();
        if (!config.fogEnabled || FogController.shouldSuppressFogForUi()) return;

        int color = config.fogColor;
        red = ((color >> 16) & 0xFF) / 255.0f;
        green = ((color >> 8) & 0xFF) / 255.0f;
        blue = (color & 0xFF) / 255.0f;
        RenderSystem.clearColor(red, green, blue, 0.0f);
    }

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void neonvisuals$customFog(Camera camera, BackgroundRenderer.FogType fogType,
                                               float viewDistance, boolean thickFog, float tickDelta,
                                               CallbackInfo ci) {
        if (fogType != BackgroundRenderer.FogType.FOG_TERRAIN) return;

        // The world behind the custom inventory must stay clear while its UI is open.
        // Use a very distant fog range instead of changing the saved fog settings.
        if (FogController.shouldSuppressFogForUi()) {
            RenderSystem.setShaderFogStart(100000.0f);
            RenderSystem.setShaderFogEnd(100001.0f);
            return;
        }

        if (VisualConfig.get().fogEnabled) {
            FogController.applyConfiguredFog(viewDistance);
        }
    }
}
