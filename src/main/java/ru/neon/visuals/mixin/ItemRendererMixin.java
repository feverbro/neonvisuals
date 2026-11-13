package ru.neon.visuals.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.VertexConsumer;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.neon.visuals.config.VisualConfig;
import ru.neon.visuals.render.FogRenderGuard;
import ru.neon.visuals.render.WhiteGlintVertexConsumer;

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

    @Redirect(
            method = "getItemGlintConsumer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"
            )
    )
    private static VertexConsumer neonvisuals$whiteItemGlint(VertexConsumerProvider provider, RenderLayer layer) {
        return neonvisuals$wrapGlint(provider, layer);
    }

    @Redirect(
            method = "getDirectItemGlintConsumer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"
            )
    )
    private static VertexConsumer neonvisuals$whiteDirectItemGlint(VertexConsumerProvider provider, RenderLayer layer) {
        return neonvisuals$wrapGlint(provider, layer);
    }

    private static VertexConsumer neonvisuals$wrapGlint(VertexConsumerProvider provider, RenderLayer layer) {
        VertexConsumer consumer = provider.getBuffer(layer);
        VisualConfig config = VisualConfig.get();
        if (!config.whiteGlint || config.glintWhiteness <= 0) return consumer;

        boolean glintLayer = layer == RenderLayer.getGlint()
                || layer == RenderLayer.getGlintTranslucent()
                || layer == RenderLayer.getEntityGlint()
                || layer == RenderLayer.getDirectEntityGlint()
                || layer == RenderLayer.getArmorEntityGlint();
        return glintLayer ? new WhiteGlintVertexConsumer(consumer, config.glintWhiteness) : consumer;
    }
}
