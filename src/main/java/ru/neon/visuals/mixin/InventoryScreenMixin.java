package ru.neon.visuals.mixin;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.render.FogRenderGuard;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    // Both InventoryScreen.drawEntity overloads are covered by the name-only selector.
    @Inject(method = "drawEntity", at = @At("HEAD"))
    private static void neonvisuals$beginPlayerPreviewWithoutFog(CallbackInfo ci) {
        FogRenderGuard.begin();
    }

    @Inject(method = "drawEntity", at = @At("RETURN"))
    private static void neonvisuals$endPlayerPreviewWithoutFog(CallbackInfo ci) {
        FogRenderGuard.end();
    }
}
