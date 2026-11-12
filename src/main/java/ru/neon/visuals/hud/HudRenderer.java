package ru.neon.visuals.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import ru.neon.visuals.NeonVisualsClient;
import ru.neon.visuals.config.VisualConfig;

import java.util.Locale;

public final class HudRenderer {
    private HudRenderer() {
    }

    public static void render(MinecraftClient client, DrawContext context) {
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        VisualConfig config = VisualConfig.get();

        drawDamagePulse(client, context, config);
        if (config.customCrosshair) drawCrosshair(client, context, config);
        if (config.hitMarker) drawHitMarker(client, context, config);

        int x = 8;
        int y = 8;
        if (config.fpsHud) y = drawInfoLine(context, x, y, "FPS", Integer.toString(client.getCurrentFps()), config);
        if (config.coordinatesHud) {
            String coords = String.format(Locale.ROOT, "%.1f  %.1f  %.1f", client.player.getX(), client.player.getY(), client.player.getZ());
            y = drawInfoLine(context, x, y, "XYZ", coords, config);
        }
        if (config.pingHud) {
            int ping = 0;
            if (client.getNetworkHandler() != null) {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            y = drawInfoLine(context, x, y, "PING", ping + " ms", config);
        }
        if (config.cpsHud) y = drawInfoLine(context, x, y, "CPS", Integer.toString(NeonVisualsClient.getCps()), config);
        if (config.totemHud) {
            int count = client.player.getInventory().count(Items.TOTEM_OF_UNDYING);
            y = drawInfoLine(context, x, y, "TOTEMS", Integer.toString(count), config);
        }
        if (config.armorHud) drawArmor(context, client, config, x, y + 2);
        if (config.keystrokesHud) drawKeystrokes(context, client, config);
    }

    private static int drawInfoLine(DrawContext context, int x, int y, String label, String value, VisualConfig config) {
        int width = 12 + MinecraftClient.getInstance().textRenderer.getWidth(label)
                + MinecraftClient.getInstance().textRenderer.getWidth(value) + 16;
        int height = config.compactHud ? 13 : 15;
        drawPanel(context, x, y, width, height, config);
        context.fill(x, y + 2, x + 2, y + height - 2, config.accentColor);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label, x + 7, y + 3, config.accentColor);
        int valueX = x + width - 6 - MinecraftClient.getInstance().textRenderer.getWidth(value);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, value, valueX, y + 3, 0xFFFFFFFF);
        return y + (config.compactHud ? 15 : 18);
    }

    private static void drawCrosshair(MinecraftClient client, DrawContext context, VisualConfig config) {
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int length = config.crosshairLength;
        int gap = config.crosshairGap;
        int width = config.crosshairWidth;
        int color = (config.crosshairOpacity << 24) | (config.accentColor & 0x00FFFFFF);

        context.fill(cx - width / 2, cy - gap - length, cx - width / 2 + width, cy - gap, color);
        context.fill(cx - width / 2, cy + gap + 1, cx - width / 2 + width, cy + gap + length + 1, color);
        context.fill(cx - gap - length, cy - width / 2, cx - gap, cy - width / 2 + width, color);
        context.fill(cx + gap + 1, cy - width / 2, cx + gap + length + 1, cy - width / 2 + width, color);
        if (config.crosshairDot) {
            context.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        }
    }

    private static void drawDamagePulse(MinecraftClient client, DrawContext context, VisualConfig config) {
        if (!config.damagePulse || client.player.hurtTime <= 0) return;
        float progress = MathHelper.clamp(client.player.hurtTime / 10.0f, 0.0f, 1.0f);
        int alpha = (int) (config.pulseStrength * progress);
        if (alpha <= 0) return;
        int color = (alpha << 24) | 0x00FF263B;
        context.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), color);
    }

    private static void drawHitMarker(MinecraftClient client, DrawContext context, VisualConfig config) {
        long elapsed = System.currentTimeMillis() - NeonVisualsClient.getLastImpactAt();
        if (elapsed < 0 || elapsed > 220) return;
        int alpha = (int) (255.0f * (1.0f - elapsed / 220.0f));
        int color = (alpha << 24) | (config.accentColor & 0x00FFFFFF);
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        for (int i = 0; i < 6; i++) {
            context.fill(cx - 9 + i, cy - 9 + i, cx - 6 + i, cy - 6 + i, color);
            context.fill(cx + 6 - i, cy - 9 + i, cx + 9 - i, cy - 6 + i, color);
            context.fill(cx - 9 + i, cy + 6 - i, cx - 6 + i, cy + 9 - i, color);
            context.fill(cx + 6 - i, cy + 6 - i, cx + 9 - i, cy + 9 - i, color);
        }
    }

    private static void drawArmor(DrawContext context, MinecraftClient client, VisualConfig config, int x, int y) {
        PlayerInventory inventory = client.player.getInventory();
        int boxWidth = 25;
        for (int slot = 3; slot >= 0; slot--) {
            int px = x + (3 - slot) * (boxWidth + 3);
            ItemStack stack = inventory.getArmorStack(slot);
            drawPanel(context, px, y, boxWidth, 26, config);
            if (!stack.isEmpty()) {
                context.drawItem(stack, px + 4, y + 3);
                int maxDamage = stack.getMaxDamage();
                if (maxDamage > 0) {
                    int remaining = maxDamage - stack.getDamage();
                    int percent = (int) (remaining * 100.0f / maxDamage);
                    int color = percent > 60 ? 0xFF63E6A5 : percent > 25 ? 0xFFFFC857 : 0xFFFF667A;
                    context.drawTextWithShadow(client.textRenderer, percent + "%", px + 2, y + 17, color);
                } else {
                    context.drawTextWithShadow(client.textRenderer, "∞", px + 9, y + 17, 0xFFFFFFFF);
                }
            } else {
                context.drawTextWithShadow(client.textRenderer, "—", px + 9, y + 9, 0xFF707A8C);
            }
        }
    }

    private static void drawKeystrokes(DrawContext context, MinecraftClient client, VisualConfig config) {
        int x = 8;
        int y = context.getScaledWindowHeight() - 70;
        drawKey(context, client, config, x + 28, y, 20, 20, "W", client.options.forwardKey.isPressed());
        drawKey(context, client, config, x + 5, y + 23, 20, 20, "A", client.options.leftKey.isPressed());
        drawKey(context, client, config, x + 28, y + 23, 20, 20, "S", client.options.backKey.isPressed());
        drawKey(context, client, config, x + 51, y + 23, 20, 20, "D", client.options.rightKey.isPressed());
        drawKey(context, client, config, x + 5, y + 46, 66, 16, "SPACE", client.options.jumpKey.isPressed());
    }

    private static void drawKey(DrawContext context, MinecraftClient client, VisualConfig config,
                                int x, int y, int width, int height, String text, boolean pressed) {
        int background = pressed ? (config.accentColor & 0xDFFFFFFF) : 0xB8141B29;
        context.fill(x, y, x + width, y + height, background);
        context.fill(x, y, x + width, y + 1, pressed ? config.accentColor : 0x553E4A5D);
        int tw = client.textRenderer.getWidth(text);
        context.drawTextWithShadow(client.textRenderer, text, x + (width - tw) / 2, y + (height - 8) / 2, 0xFFFFFFFF);
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height, VisualConfig config) {
        int alpha = config.panelOpacity;
        context.fill(x, y, x + width, y + height, (alpha << 24) | 0x00101827);
        context.fill(x, y, x + width, y + 1, (config.accentColor & 0x00FFFFFF) | 0x66000000);
        context.fill(x, y + height - 1, x + width, y + height, 0x332E3A4E);
    }
}
