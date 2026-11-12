package ru.neon.visuals.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.neon.visuals.config.VisualConfig;
import ru.neon.visuals.render.WhiteGlintVertexConsumer;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
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
