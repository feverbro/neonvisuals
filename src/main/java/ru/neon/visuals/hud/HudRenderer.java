package ru.neon.visuals.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.EntityHitResult;
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
        boolean rightHudVisible = (config.targetHud && NeonVisualsClient.getTrackedTarget() != null)
                || (config.potionHud && !client.player.getStatusEffects().isEmpty());
        int maxInfoWidth = Math.max(76, context.getScaledWindowWidth() - 24 - (rightHudVisible ? 190 : 0));
        if (config.fpsHud) y = drawInfoLine(context, x, y, "FPS", Integer.toString(client.getCurrentFps()), config, maxInfoWidth);
        if (config.coordinatesHud) {
            String coords = String.format(Locale.ROOT, "%.1f  %.1f  %.1f", client.player.getX(), client.player.getY(), client.player.getZ());
            y = drawInfoLine(context, x, y, "XYZ", coords, config, maxInfoWidth);
        }
        if (config.pingHud) {
            int ping = 0;
            if (client.getNetworkHandler() != null) {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            y = drawInfoLine(context, x, y, "PING", ping + " ms", config, maxInfoWidth);
        }
        if (config.cpsHud) y = drawInfoLine(context, x, y, "CPS", Integer.toString(NeonVisualsClient.getCps()), config, maxInfoWidth);
        if (config.totemHud) {
            int count = client.player.getInventory().count(Items.TOTEM_OF_UNDYING);
            y = drawInfoLine(context, x, y, "TOTEMS", Integer.toString(count), config, maxInfoWidth);
        }
        int keystrokesTop = context.getScaledWindowHeight() - 70;
        if (config.armorHud && y + 28 <= keystrokesTop - 4) drawArmor(context, client, config, x, y + 2);
        if (config.keystrokesHud) drawKeystrokes(context, client, config);
        if (config.targetHud) drawTargetHud(context, client, config);
        if (config.potionHud) drawPotionHud(context, client, config);
    }

    private static int drawInfoLine(DrawContext context, int x, int y, String label, String value,
                                     VisualConfig config, int maxWidth) {
        var renderer = MinecraftClient.getInstance().textRenderer;
        int labelWidth = renderer.getWidth(label);
        int maxValueWidth = Math.max(0, maxWidth - 38 - labelWidth);
        String shownValue = renderer.trimToWidth(value, maxValueWidth);
        int valueWidth = renderer.getWidth(shownValue);
        int width = Math.min(maxWidth, 18 + labelWidth + valueWidth + 20);
        int height = config.compactHud ? 15 : 18;
        drawPanel(context, x, y, width, height, config);
        context.fill(x + 1, y + 3, x + 3, y + height - 3, config.accentColor);
        context.drawTextWithShadow(renderer, label, x + 8, y + 4, config.accentColor);
        int valueX = x + width - 8 - valueWidth;
        if (!shownValue.isEmpty()) {
            context.drawTextWithShadow(renderer, shownValue, valueX, y + 4, 0xFFF4F7FC);
        }
        return y + (config.compactHud ? 17 : 21);
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
                    context.drawTextWithShadow(client.textRenderer, client.textRenderer.trimToWidth(percent + "%", boxWidth - 4), px + 2, y + 17, color);
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

    private static void drawTargetHud(DrawContext context, MinecraftClient client, VisualConfig config) {
        LivingEntity target = NeonVisualsClient.getTrackedTarget();
        if (target == null) return;

        int x = context.getScaledWindowWidth() - 184;
        int y = 10;
        int width = 174;
        int height = 66;
        drawHudCard(context, x, y, width, height, config);
        context.fill(x, y + 1, x + 3, y + height - 1, config.accentColor);
        context.drawTextWithShadow(client.textRenderer, "TARGET", x + 10, y + 7, config.accentColor);

        String name = target.getDisplayName().getString();
        int maxNameWidth = 112;
        while (client.textRenderer.getWidth(name) > maxNameWidth && name.length() > 4) {
            name = name.substring(0, name.length() - 2) + "…";
        }
        context.drawTextWithShadow(client.textRenderer, name, x + 10, y + 21, 0xFFF4F7FC);

        String timer = NeonVisualsClient.getTargetHudSecondsRemaining() + "s";
        int timerWidth = client.textRenderer.getWidth(timer);
        context.drawTextWithShadow(client.textRenderer, timer, x + width - timerWidth - 9, y + 7, 0xFF9AAAC0);

        float health = Math.max(0.0f, target.getHealth());
        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        float ratio = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);
        int hpColor = ratio > 0.60f ? 0xFF55DDA1 : ratio > 0.30f ? 0xFFFFC75E : 0xFFFF667A;
        String healthText = String.format(Locale.ROOT, "%.1f HP", health);
        String distance = String.format(Locale.ROOT, "%.1fm", client.player.distanceTo(target));
        int distanceWidth = client.textRenderer.getWidth(distance);
        int healthMaxWidth = Math.max(0, width - 28 - distanceWidth);
        healthText = client.textRenderer.trimToWidth(healthText, healthMaxWidth);
        if (!healthText.isEmpty()) {
            context.drawTextWithShadow(client.textRenderer, healthText, x + 10, y + 37, hpColor);
        }
        context.drawTextWithShadow(client.textRenderer, distance, x + width - distanceWidth - 9, y + 37, 0xFF9AAAC0);

        int barX = x + 10;
        int barY = y + 53;
        int barWidth = width - 20;
        context.fill(barX, barY, barX + barWidth, barY + 5, 0xFF263143);
        if (ratio > 0.0f) context.fill(barX, barY, barX + (int) (barWidth * ratio), barY + 5, hpColor);
    }

    private static void drawPotionHud(DrawContext context, MinecraftClient client, VisualConfig config) {
        var effects = client.player.getStatusEffects();
        if (effects.isEmpty()) return;

        int x = context.getScaledWindowWidth() - 184;
        int y = config.targetHud && NeonVisualsClient.getTrackedTarget() != null ? 84 : 10;
        int width = 174;
        int availableRows = Math.max(1, (context.getScaledWindowHeight() - y - 30) / 18);
        int count = Math.min(Math.min(6, effects.size()), availableRows);
        int height = 18 + count * 18;
        drawHudCard(context, x, y, width, height, config);
        context.drawTextWithShadow(client.textRenderer, "ACTIVE EFFECTS", x + 9, y + 6, config.accentColor);

        int row = 0;
        for (StatusEffectInstance effect : effects) {
            if (row >= 6) break;
            int rowY = y + 20 + row * 18;
            int color = 0xFF000000 | effect.getEffectType().value().getColor();
            context.fill(x + 9, rowY + 2, x + 12, rowY + 13, color);

            String name = effect.getEffectType().value().getName().getString();
            String level = romanLevel(effect.getAmplifier() + 1);
            String duration = effect.isInfinite() ? "∞" : formatDuration(effect.getDuration());
            int durationWidth = client.textRenderer.getWidth(duration);
            int allowed = Math.max(0, width - 32 - durationWidth);
            String effectLabel = client.textRenderer.trimToWidth(name + " " + level, allowed);
            if (!effectLabel.isEmpty()) {
                context.drawTextWithShadow(client.textRenderer, effectLabel, x + 16, rowY + 3, 0xFFEAF0F8);
            }
            context.drawTextWithShadow(client.textRenderer, duration, x + width - 8 - durationWidth,
                    rowY + 3, 0xFF9EACC0);
            row++;
        }
    }

    private static String romanLevel(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(level);
        };
    }

    private static String formatDuration(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private static void drawHudCard(DrawContext context, int x, int y, int width, int height, VisualConfig config) {
        int alpha = Math.min(238, Math.max(165, config.panelOpacity));
        context.fill(x + 3, y + 3, x + width + 3, y + height + 3, 0x55000000);
        context.fill(x, y, x + width, y + height, (alpha << 24) | 0x0008121F);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, (config.accentColor & 0x00FFFFFF) | 0xCC000000);
        context.fill(x, y + height - 1, x + width, y + height, 0x66475A73);
        context.fill(x, y, x + 1, y + height, 0x774A5C75);
        context.fill(x + width - 1, y, x + width, y + height, 0x774A5C75);
        context.fill(x + 5, y + height - 2, x + width - 5, y + height - 1, 0x223F5874);
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
        context.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x44000000);
        context.fill(x, y, x + width, y + height, (alpha << 24) | 0x000B1422);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, (config.accentColor & 0x00FFFFFF) | 0x99000000);
        context.fill(x, y + height - 1, x + width, y + height, 0x55465A72);
        context.fill(x, y, x + 1, y + height, 0x66465A72);
        context.fill(x + width - 1, y, x + width, y + height, 0x66465A72);
    }
}
