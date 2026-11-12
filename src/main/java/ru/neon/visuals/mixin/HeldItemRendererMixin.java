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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.NeonVisualsClient;
import ru.neon.visuals.config.VisualConfig;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    /**
     * Replace the exact vanilla three-axis swing translation in
     * renderFirstPersonItem. For an idle sword, Minecraft computes X/Y/Z from
     * swingProgress and sends them through this MatrixStack.translate call.
     * Replace that vector with a pure horizontal X displacement instead.
     */
    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V"
            )
    )
    private void neonvisuals$replaceSwordSwingTranslation(
            MatrixStack matrices,
            double vanillaX,
            double vanillaY,
            double vanillaZ,
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack renderMatrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        VisualConfig config = VisualConfig.get();

        // Preserve all use-action transforms. The idle sword branch uses the
        // vanilla three-axis translation solely for the attack swing.
        if (config.swordAdjust && item != null && item.getItem() instanceof SwordItem
                && !player.isUsingItem()) {
            float pulse = config.swordSway ? NeonVisualsClient.getSwordSwingOffset() : 0.0f;
            float distance = 0.28f + config.swordSwayAngle * 0.035f;
            double screenHorizontal = -pulse * distance;
            if (hand == Hand.OFF_HAND) screenHorizontal = -screenHorizontal;

            // Replace vanilla (X,Y,Z) with horizontal-only movement.
            matrices.translate(screenHorizontal, 0.0, 0.0);
            return;
        }

        matrices.translate(vanillaX, vanillaY, vanillaZ);
    }

    /** Cancel vanilla arm swing rotation for the sword hand; keep equip motion. */
    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void neonvisuals$cancelVanillaSwordArmSwing(MatrixStack matrices, Arm arm,
                                                         float swingProgress, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualConfig.get().swordAdjust || client.player == null) return;

        boolean mainHand = arm == client.player.getMainArm();
        ItemStack held = mainHand
                ? client.player.getMainHandStack()
                : client.player.getOffHandStack();
        if (held.getItem() instanceof SwordItem) ci.cancel();
    }

    /** Apply only static sword position/rotation/scale settings during item rendering. */
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
