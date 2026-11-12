package ru.neon.visuals.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.config.VisualConfig;
import ru.neon.visuals.render.FogController;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void neonvisuals$customFog(Camera camera, BackgroundRenderer.FogType fogType,
                                               float viewDistance, boolean thickFog, float tickDelta,
                                               CallbackInfo ci) {
        // Apply the same configured fog to both terrain and sky passes. Restricting
        // this to FOG_TERRAIN makes the effect look like a block-only overlay.
        if (VisualConfig.get().fogEnabled) {
            FogController.applyConfiguredFog();
        }
    }
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private static void neonvisuals$blendSkyToFog(Camera camera, float tickDelta,
                                                  CallbackInfoReturnable<Vec3d> cir) {
        VisualConfig config = VisualConfig.get();
        if (!config.fogEnabled) return;
        int color = config.fogColor;
        cir.setReturnValue(new Vec3d(
                ((color >> 16) & 0xFF) / 255.0,
                ((color >> 8) & 0xFF) / 255.0,
                (color & 0xFF) / 255.0
        ));
    }
}
