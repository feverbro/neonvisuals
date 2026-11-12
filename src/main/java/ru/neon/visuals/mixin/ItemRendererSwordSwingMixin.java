package ru.neon.visuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
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
import org.spongepowered.asm.mixin.Unique;
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
    @Unique private boolean neonvisuals$shiftPushed;
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
        neonvisuals$shiftPushed = false;
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
        Matrix4f modelViewAndItem = new Matrix4f(RenderSystem.getModelViewMatrix())
                .mul(new Matrix4f(matrices.peek().getPositionMatrix()));
        modelViewAndItem.invert().transformDirection(localShift);

        matrices.push();
        neonvisuals$shiftPushed = true;
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
        if (neonvisuals$shiftPushed) {
            matrices.pop();
            neonvisuals$shiftPushed = false;
        }
    }
}
