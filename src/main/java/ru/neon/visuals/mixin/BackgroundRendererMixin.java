package ru.neon.visuals.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.config.VisualConfig;
import ru.neon.visuals.render.FogController;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void neonvisuals$customFog(Camera camera, BackgroundRenderer.FogType fogType,
                                               float viewDistance, boolean thickFog, float tickDelta,
                                               CallbackInfo ci) {
        // Apply the configured fog to both terrain and sky fog passes.
        if (VisualConfig.get().fogEnabled) {
            FogController.applyConfiguredFog();
        }
    }
}
