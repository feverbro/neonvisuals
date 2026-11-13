package ru.neon.visuals.mixin;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.render.FogRenderGuard;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD")
    )
    private void neonvisuals$beginItemWithoutFog(ItemStack stack, ModelTransformationMode renderMode,
                                                  boolean leftHanded, MatrixStack matrices,
                                                  VertexConsumerProvider vertexConsumers, int light,
                                                  int overlay, BakedModel model, CallbackInfo ci) {
        // Flush previously queued world geometry before changing the global fog state.
        if (vertexConsumers instanceof Immediate immediate) immediate.draw();
        FogRenderGuard.begin();
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("RETURN")
    )
    private void neonvisuals$endItemWithoutFog(ItemStack stack, ModelTransformationMode renderMode,
                                                boolean leftHanded, MatrixStack matrices,
                                                VertexConsumerProvider vertexConsumers, int light,
                                                int overlay, BakedModel model, CallbackInfo ci) {
        // Item vertices are buffered; flush them while the custom fog is still disabled.
        if (vertexConsumers instanceof Immediate immediate) immediate.draw();
        FogRenderGuard.end();
    }

}
