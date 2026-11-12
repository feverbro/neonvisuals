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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.NeonVisualsClient;
import ru.neon.visuals.config.VisualConfig;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    /**
     * Create an outer matrix scope around this hand render. The custom swing
     * translation is applied before any of Minecraft's hand/item rotations,
     * so it stays on the camera-horizontal axis rather than following the
     * rotated sword model's local axes.
     */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void neonvisuals$beginSwordRender(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        matrices.push();
        if (VisualConfig.get().swordAdjust && item != null && item.getItem() instanceof SwordItem) {
            float pulse = VisualConfig.get().swordSway
                    ? NeonVisualsClient.getSwordSwingOffset() : 0.0f;
            float distance = 0.28f + VisualConfig.get().swordSwayAngle * 0.035f;
            float direction = hand == Hand.MAIN_HAND ? -1.0f : 1.0f;
            // MatrixStack is still at the camera/view transform here: this X
            // shift is established before vanilla hand pitch/roll transforms.
            matrices.translate(direction * pulse * distance, 0.0, 0.0);
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void neonvisuals$endSwordRender(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        matrices.pop();
    }

    /**
     * Suppress the vanilla swing progress for the exact sword stack being
     * rendered. Keep equip progress, which is the separate slot-switch motion.
     */
    @ModifyVariable(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2
    )
    private float neonvisuals$removeVanillaSwordSwing(
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
     * The remaining vanilla swing helper is also disabled for the sword hand.
     * This does not affect equip lowering/raising.
     */
    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void neonvisuals$cancelVanillaSwordSwing(MatrixStack matrices, Arm arm,
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
            // Static user adjustments only. The attack animation is applied
            // once, at renderFirstPersonItem HEAD, in camera-horizontal space.
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
