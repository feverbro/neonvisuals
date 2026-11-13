package ru.neon.visuals.mixin;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.render.FogRenderGuard;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD")
    )
    private static void neonvisuals$beginPlayerPreviewWithoutFog(CallbackInfo ci) {
        FogRenderGuard.begin();
    }

    @Inject(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("RETURN")
    )
    private static void neonvisuals$endPlayerPreviewWithoutFog(CallbackInfo ci) {
        FogRenderGuard.end();
    }

    @Inject(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD")
    )
    private static void neonvisuals$beginModernPlayerPreviewWithoutFog(CallbackInfo ci) {
        FogRenderGuard.begin();
    }

    @Inject(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V",
            at = @At("RETURN")
    )
    private static void neonvisuals$endModernPlayerPreviewWithoutFog(CallbackInfo ci) {
        FogRenderGuard.end();
    }
}
