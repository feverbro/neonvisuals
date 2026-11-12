package ru.neon.visuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
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
     * Zero the attack progress at method entry so vanilla does not apply its
     * earlier swing-dependent position changes to a sword. Slot equip motion
     * remains independent and is preserved.
     */
    @ModifyVariable(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2
    )
    private float neonvisuals$removeVanillaSwordSwingProgress(
            float swingProgress,
            net.minecraft.client.network.AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            net.minecraft.util.Hand hand,
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
     * Also cancel the dedicated vanilla arm-swing transform for the hand that
     * actually holds a sword. This is separate from the equip/slot animation.
     */
    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
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
            // Static position/rotation/scale options are independent from the
            // attack motion and are applied first.
            matrices.translate(config.swordX / 100.0, config.swordY / 100.0, config.swordZ / 100.0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(config.swordRotation));
            float scale = config.swordScale / 100.0f;
            matrices.scale(scale, scale, scale);

            float pulse = config.swordSway ? NeonVisualsClient.getSwordSwingOffset() : 0.0f;
            float distance = 0.28f + config.swordSwayAngle * 0.035f;
            float screenLeft = -pulse * distance;

            /*
             * Convert a pure view-space X translation into the coordinate
             * space of this item matrix:
             *
             *   V * M' = T(view-X) * V * M
             *   M' = inverse(V) * T(view-X) * V * M
             *
             * This is invariant under camera pitch, yaw, hand pose, and item
             * rotations. It avoids using the sword's rotated local X/Y axes.
             */
            Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());
            Matrix4f inverseView = new Matrix4f(view).invert();
            Matrix4f position = matrices.peek().getPositionMatrix();
            Matrix4f viewTranslation = new Matrix4f().translation(screenLeft, 0.0f, 0.0f);
            Matrix4f adjusted = new Matrix4f(inverseView)
                    .mul(viewTranslation)
                    .mul(view)
                    .mul(new Matrix4f(position));
            position.set(adjusted);

            renderer.renderItem(entity, stack, mode, leftHanded, matrices, consumers, light);
        } finally {
            matrices.pop();
        }
    }
}
