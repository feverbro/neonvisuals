package ru.neon.visuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.NeonVisualsClient;
import ru.neon.visuals.config.VisualConfig;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    /**
     * Remove vanilla attack progress before renderFirstPersonItem computes
     * its early swing translation. The equip progress remains untouched.
     */
    @ModifyVariable(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2
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
     * Cancel the dedicated vanilla swing transform for the hand holding the
     * sword. This is separate from the equip/slot-switch animation.
     */
    @org.spongepowered.asm.mixin.injection.Inject(
            method = "applySwingOffset",
            at = @At("HEAD"),
            cancellable = true
    )
    private void neonvisuals$cancelVanillaSwordArmSwing(MatrixStack matrices, Arm arm,
                                                         float swingProgress, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualConfig.get().swordAdjust || client.player == null) return;

        boolean mainHand = arm == client.player.getMainArm();
        ItemStack held = mainHand
                ? client.player.getMainHandStack()
                : client.player.getOffHandStack();

        if (held.getItem() instanceof SwordItem) {
            ci.cancel();
        }
    }

    /**
     * This redirect executes at the renderItem call, after vanilla has applied
     * the hand/equip transforms. Since sword swing progress and applySwingOffset
     * are already suppressed, the current matrix has no attack rotation.
     *
     * Translate only X here. Do not modify the matrix at renderFirstPersonItem
     * HEAD: later vanilla transforms would rotate that earlier displacement.
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

            // A pure horizontal translation at the final hand-render stage.
            // The vanilla transforms that used to turn this into Y/Z motion
            // have already run, and the sword's own rotation is applied later.
            matrices.translate(-pulse * distance, 0.0, 0.0);

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
