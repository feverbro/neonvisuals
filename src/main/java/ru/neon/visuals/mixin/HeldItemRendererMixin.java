package ru.neon.visuals.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.neon.visuals.NeonVisualsClient;
import ru.neon.visuals.config.VisualConfig;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    /**
     * Remove the vanilla sword swing input before vanilla applies its
     * three-axis arm/item swing transform.
     */
    @ModifyVariable(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            argsOnly = true,
            index = 5
    )
    private float neonvisuals$removeVanillaSwordSwingProgress(
            float swingProgress,
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float originalSwingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        return item != null && item.getItem() instanceof SwordItem ? 0.0f : swingProgress;
    }

    /**
     * Apply the custom movement in camera space, not in the already-rotated
     * item-local axes. The inverse current pose converts a camera-horizontal
     * vector into the local translation required by MatrixStack.translate.
     */
    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    private void neonvisuals$adjustSword(HeldItemRenderer renderer, LivingEntity entity, ItemStack stack,
                                         ModelTransformationMode mode, boolean leftHanded,
                                         MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        VisualConfig config = VisualConfig.get();
        if (!config.swordAdjust || !(stack.getItem() instanceof SwordItem)) {
            renderer.renderItem(entity, stack, mode, leftHanded, matrices, consumers, light);
            return;
        }

        matrices.push();
        try {
            float pulse = config.swordSway ? NeonVisualsClient.getSwordSwingOffset() : 0.0f;
            float distance = 0.28f + config.swordSwayAngle * 0.035f;

            // Move only along the X coordinate; Y and Z stay unchanged.
            float cameraX = -pulse * distance;
            matrices.translate(cameraX, 0.0, 0.0);

            // Static view-model settings remain independent of the swing.
            matrices.translate(config.swordX / 100.0, config.swordY / 100.0, config.swordZ / 100.0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(config.swordRotation));
            float scale = config.swordScale / 100.0f;
            matrices.scale(scale, scale, scale);
            renderer.renderItem(entity, stack, mode, leftHanded, matrices, consumers, light);
        } finally {
            matrices.pop();
        }
    }
}
