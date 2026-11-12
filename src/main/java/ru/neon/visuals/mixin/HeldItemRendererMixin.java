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
     * In 1.21 renderFirstPersonItem adds an extra (X,Y,Z) translation from
     * swingProgress before applySwingOffset is called. Cancel that earlier
     * translation too, otherwise the sword still drops vertically.
     */
    @ModifyVariable(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2
    )
    private float neonvisuals$zeroSwordSwingProgress(float swingProgress) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualConfig.get().swordAdjust || client.player == null) return swingProgress;

        if (client.player.getMainHandStack().getItem() instanceof SwordItem
                || client.player.getOffHandStack().getItem() instanceof SwordItem) {
            return 0.0f;
        }
        return swingProgress;
    }

    /**
     * Cancel the vanilla arm swing transform only for the arm holding a sword.
     * Equip lowering/raising is not touched.
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
            // Apply permanent item tuning first; these offsets are static and do
            // not participate in the attack path.
            matrices.translate(config.swordX / 100.0, config.swordY / 100.0, config.swordZ / 100.0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(config.swordRotation));
            float scale = config.swordScale / 100.0f;
            matrices.scale(scale, scale, scale);

            float swingOffset = config.swordSway ? NeonVisualsClient.getSwordSwingOffset() : 0.0f;
            float swingDistance = 0.28f + config.swordSwayAngle * 0.035f;

            // Directly adjust the camera-space X translation only. Do not
            // translate along item-local axes or alter Y/Z, so the path is
            // strictly horizontal regardless of arm pitch or item rotation.
            Matrix4f position = matrices.peek().getPositionMatrix();
            position.m30(position.m30() - swingOffset * swingDistance);

            renderer.renderItem(entity, stack, mode, leftHanded, matrices, consumers, light);
        } finally {
            matrices.pop();
        }
    }
}
