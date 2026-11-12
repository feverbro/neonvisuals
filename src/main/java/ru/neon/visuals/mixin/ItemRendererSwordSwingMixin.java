package ru.neon.visuals.mixin;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.NeonVisualsClient;
import ru.neon.visuals.config.VisualConfig;

/**
 * Applies the custom sword movement at the final baked-model stage, after
 * first-person hand and item-model transformations have already been applied.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererSwordSwingMixin {
    @Inject(method = "renderBakedItemModel", at = @At("HEAD"))
    private void neonvisuals$beginHorizontalSwordShift(
            BakedModel model,
            ItemStack stack,
            int light,
            int overlay,
            MatrixStack matrices,
            VertexConsumer vertices,
            CallbackInfo ci
    ) {
        VisualConfig config = VisualConfig.get();
        if (!config.swordAdjust || !config.swordSway || !(stack.getItem() instanceof SwordItem)) return;

        float pulse = NeonVisualsClient.getSwordSwingOffset();
        if (pulse <= 0.0f) return;

        float distance = 0.28f + config.swordSwayAngle * 0.035f;
        float screenX = -pulse * distance;

        // At this point the matrix already contains the camera, hand and
        // first-person item-model rotations. Convert desired camera-space X
        // into the matching local vector using the inverse 3x3 basis.
        Vector3f localShift = new Vector3f(screenX, 0.0f, 0.0f);
        Matrix4f inverse = new Matrix4f(matrices.peek().getPositionMatrix()).invert();
        inverse.transformDirection(localShift);

        matrices.push();
        matrices.translate(localShift.x, localShift.y, localShift.z);
    }

    @Inject(method = "renderBakedItemModel", at = @At("RETURN"))
    private void neonvisuals$endHorizontalSwordShift(
            BakedModel model,
            ItemStack stack,
            int light,
            int overlay,
            MatrixStack matrices,
            VertexConsumer vertices,
            CallbackInfo ci
    ) {
        VisualConfig config = VisualConfig.get();
        if (!config.swordAdjust || !config.swordSway || !(stack.getItem() instanceof SwordItem)) return;

        // Only pop when a non-zero pulse caused the matching push.
        if (NeonVisualsClient.getSwordSwingOffset() > 0.0f) {
            matrices.pop();
        }
    }
}
