package ru.neon.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import ru.neon.visuals.config.VisualConfig;
import ru.neon.visuals.gui.VisualMenuScreen;
import ru.neon.visuals.hud.HudRenderer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public final class NeonVisualsClient implements ClientModInitializer {
    private static KeyBinding menuKey;
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Random RANDOM = new Random();
    private static long lastImpactAt;
    private static boolean wasOnGround;
    private static int tickCounter;

    @Override
    public void onInitializeClient() {
        VisualConfig.load();
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.neonvisuals.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.neonvisuals.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen instanceof VisualMenuScreen) {
                    client.setScreen(null);
                } else if (client.currentScreen == null) {
                    client.setScreen(new VisualMenuScreen());
                }
            }
            tick(client);
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> HudRenderer.render(MinecraftClient.getInstance(), context));
    }

    private static void tick(MinecraftClient client) {
        tickCounter++;
        long now = System.currentTimeMillis();
        while (!LEFT_CLICKS.isEmpty() && now - LEFT_CLICKS.peekFirst() > 1000L) {
            LEFT_CLICKS.removeFirst();
        }

        if (client.player == null || client.world == null) {
            wasOnGround = false;
            return;
        }

        boolean onGround = client.player.isOnGround();
        if (VisualConfig.get().jumpCircle && wasOnGround && !onGround && client.player.getVelocity().y > 0.05) {
            spawnJumpCircle(client);
        }
        wasOnGround = onGround;

        if (VisualConfig.get().playerTrail && tickCounter % 3 == 0 && !onGround) {
            client.world.addParticle(
                    ParticleTypes.END_ROD,
                    client.player.getX(),
                    client.player.getY() + 0.12,
                    client.player.getZ(),
                    0.0, 0.025, 0.0
            );
        }
    }

    private static void spawnJumpCircle(MinecraftClient client) {
        double x = client.player.getX();
        double y = client.player.getY() + 0.05;
        double z = client.player.getZ();
        int amount = 28;
        for (int i = 0; i < amount; i++) {
            double angle = (Math.PI * 2.0 * i) / amount;
            double px = x + Math.cos(angle) * 0.78;
            double pz = z + Math.sin(angle) * 0.78;
            client.world.addParticle(ParticleTypes.END_ROD, px, y, pz, 0.0, 0.012, 0.0);
        }
    }

    /** Called by the Mouse mixin. This is a local visual prediction only. */
    public static void onLeftMousePressed() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return;

        long now = System.currentTimeMillis();
        LEFT_CLICKS.addLast(now);

        VisualConfig config = VisualConfig.get();
        if ((!config.hitParticles && !config.hitMarker) || client.player == null || client.world == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity target)) return;

        lastImpactAt = now;
        if (!config.hitParticles) return;
        int amount = config.particleAmount;
        for (int i = 0; i < amount; i++) {
            double vx = (RANDOM.nextDouble() - 0.5) * 0.24;
            double vy = RANDOM.nextDouble() * 0.22;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.24;
            double px = target.getX() + (RANDOM.nextDouble() - 0.5) * target.getWidth();
            double py = target.getY() + target.getHeight() * (0.35 + RANDOM.nextDouble() * 0.45);
            double pz = target.getZ() + (RANDOM.nextDouble() - 0.5) * target.getWidth();
            client.world.addParticle(ParticleTypes.CRIT, px, py, pz, vx, vy, vz);
        }
    }

    public static int getCps() {
        long now = System.currentTimeMillis();
        while (!LEFT_CLICKS.isEmpty() && now - LEFT_CLICKS.peekFirst() > 1000L) {
            LEFT_CLICKS.removeFirst();
        }
        return LEFT_CLICKS.size();
    }

    public static long getLastImpactAt() {
        return lastImpactAt;
    }
}
