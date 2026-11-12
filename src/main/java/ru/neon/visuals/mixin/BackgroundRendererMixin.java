package ru.neon.visuals.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
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

    @Inject(method = "render", at = @At("TAIL"))
    private static void neonvisuals$setHorizonFogColor(Camera camera, float tickDelta,
                                                       ClientWorld world, int viewDistance,
                                                       float skyDarkness, CallbackInfo ci) {
        VisualConfig config = VisualConfig.get();
        if (!config.fogEnabled) return;

        int color = config.fogColor;
        red = ((color >> 16) & 0xFF) / 255.0f;
        green = ((color >> 8) & 0xFF) / 255.0f;
        blue = (color & 0xFF) / 255.0f;
        FogController.applyConfiguredFog();
    }

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void neonvisuals$customFog(Camera camera, BackgroundRenderer.FogType fogType,
                                               float viewDistance, boolean thickFog, float tickDelta,
                                               CallbackInfo ci) {
        if (VisualConfig.get().fogEnabled) {
            FogController.applyConfiguredFog();
        }
    }
}
