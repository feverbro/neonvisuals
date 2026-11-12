package ru.neon.visuals.render;

import net.minecraft.client.render.VertexConsumer;

/** Tints enchantment-glint vertices toward white without affecting the item's base layer. */
public final class WhiteGlintVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int whiteness;

    public WhiteGlintVertexConsumer(VertexConsumer delegate, int whiteness) {
        this.delegate = delegate;
        this.whiteness = Math.max(0, Math.min(100, whiteness));
    }

    private int lighten(int component) {
        return component + (255 - component) * whiteness / 100;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(lighten(red), lighten(green), lighten(blue), alpha);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }
}
