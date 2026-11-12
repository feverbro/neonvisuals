package ru.neon.visuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
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
     * Remove vanilla attack progress before renderFirstPersonItem applies any
     * swing-dependent transforms. Equip progress remains untouched.
     *
     * In the 1.21 method descriptor the float arguments are tickDelta, pitch,
     * swingProgress and equipProgress, so swingProgress has float ordinal 2.
     */
    @ModifyVariable(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2
    )
    private float neonvisuals$removeVanillaSwordSwing(float swingProgress) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualConfig.get().swordAdjust || client.player == null) return swingProgress;

        // The method renders one hand at a time; check both held stacks because
        // the method's hand argument is not needed for the main-hand sword case.
        if (client.player.getMainHandStack().getItem() instanceof SwordItem
                || client.player.getOffHandStack().getItem() instanceof SwordItem) {
            return 0.0f;
        }
        return swingProgress;
    }

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
            float swingOffset = config.swordSway ? NeonVisualsClient.getSwordSwingOffset() : 0.0f;
            float handDirection = leftHanded ? -1.0f : 1.0f;
            float swingDistance = 0.28f + config.swordSwayAngle * 0.035f;
            matrices.translate(
                    config.swordX / 100.0 - swingOffset * swingDistance,
                    config.swordY / 100.0,
                    config.swordZ / 100.0
            );
            float swingRotation = swingOffset * (12.0f + config.swordSwayAngle * 2.0f) * handDirection;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(swingRotation));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(config.swordRotation));
            float scale = config.swordScale / 100.0f;
            matrices.scale(scale, scale, scale);
            renderer.renderItem(entity, stack, mode, leftHanded, matrices, consumers, light);
        } finally {
            matrices.pop();
        }
    }
}
