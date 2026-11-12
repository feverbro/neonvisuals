package ru.neon.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.item.SwordItem;
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
    private static long lastSwordSwingAtNanos;
    private static LivingEntity trackedTarget;
    private static long trackedTargetExpiresAt;
    private static long lastParticleBurstAt;
    private static int lastParticleTargetId = -1;
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

        if (client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living && living != client.player) {
            rememberTarget(living);
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

        // Trigger from the actual mouse event; this also works when the player swings at air.
        triggerSwordSwing();

        // Fallback path: the mouse event can happen before the interaction manager callback.
        VisualConfig config = VisualConfig.get();
        if (config.hitParticles && client.player != null && client.world != null
                && client.crosshairTarget instanceof EntityHitResult hit) {
            spawnHitBurst(hit.getEntity());
        }
    }

    /** Called after the client attack method has processed a melee attack attempt. */
    public static void onEntityAttackAttempt(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig config = VisualConfig.get();
        if (client.player == null || client.world == null || target == null || target.isRemoved()) return;

        lastImpactAt = System.currentTimeMillis();
        if (target instanceof LivingEntity living && living != client.player) rememberTarget(living);
        // Mouse input normally starts this first; keep the interaction hook as a fallback.
        if (System.nanoTime() - lastSwordSwingAtNanos > 120_000_000L) triggerSwordSwing();
        if (config.hitParticles) spawnHitBurst(target);
    }

    /** Spawn a visible local burst. A short per-target guard prevents mouse + attack hooks doubling it. */
    private static void spawnHitBurst(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig config = VisualConfig.get();
        if (client.player == null || client.world == null || !(target instanceof LivingEntity living) || target.isRemoved()) return;

        long now = System.currentTimeMillis();
        if (lastParticleTargetId == target.getId() && now - lastParticleBurstAt < 110L) return;
        lastParticleTargetId = target.getId();
        lastParticleBurstAt = now;
        lastImpactAt = now;

        var box = living.getBoundingBox();
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        double radius = Math.max(0.35, Math.max(box.maxX - box.minX, box.maxZ - box.minZ) * 0.75);
        double spread = config.particleSpread / 100.0;
        double speed = config.particleSpeed / 100.0;
        int amount = Math.max(12, config.particleAmount);

        // Bright rotating-looking ring of crit and enchantment sparks.
        for (int i = 0; i < amount; i++) {
            double angle = (Math.PI * 2.0 * i) / amount;
            double ring = radius * (0.75 + RANDOM.nextDouble() * 0.35);
            double px = centerX + Math.cos(angle) * ring;
            double py = box.minY + living.getHeight() * (0.28 + RANDOM.nextDouble() * 0.54);
            double pz = centerZ + Math.sin(angle) * ring;
            double outward = (0.07 + spread * 0.13) * speed;
            double vx = Math.cos(angle) * outward + (RANDOM.nextDouble() - 0.5) * 0.035;
            double vy = (0.07 + RANDOM.nextDouble() * 0.20) * speed;
            double vz = Math.sin(angle) * outward + (RANDOM.nextDouble() - 0.5) * 0.035;
            client.world.addParticle(i % 3 == 0 ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT,
                    px, py, pz, vx, vy, vz);
        }

        // A smaller outer halo of long-lived white sparks.
        int halo = Math.max(6, amount / 3);
        for (int i = 0; i < halo; i++) {
            double angle = (Math.PI * 2.0 * i) / halo;
            double px = centerX + Math.cos(angle) * radius * 0.55;
            double py = box.minY + living.getHeight() * (0.45 + RANDOM.nextDouble() * 0.25);
            double pz = centerZ + Math.sin(angle) * radius * 0.55;
            double velocity = (0.035 + spread * 0.055) * speed;
            client.world.addParticle(ParticleTypes.END_ROD, px, py, pz,
                    Math.cos(angle) * velocity, 0.045 * speed, Math.sin(angle) * velocity);
        }

        // Vanilla sweep slash makes the hit unmistakable at close range.
        client.world.addParticle(ParticleTypes.SWEEP_ATTACK,
                centerX, box.minY + 0.12, centerZ, 0.0, 0.0, 0.0);
    }

    private static void rememberTarget(LivingEntity target) {
        if (target == null || target.isRemoved() || !target.isAlive()) return;
        trackedTarget = target;
        trackedTargetExpiresAt = System.currentTimeMillis() + 30_000L;
    }

    /** Last aimed-at/attacked entity, retained for 30 seconds. */
    public static LivingEntity getTrackedTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (trackedTarget == null || trackedTarget.isRemoved() || !trackedTarget.isAlive()
                || client.world == null || trackedTarget.getWorld() != client.world
                || System.currentTimeMillis() > trackedTargetExpiresAt) {
            trackedTarget = null;
            return null;
        }
        return trackedTarget;
    }

    public static int getTargetHudSecondsRemaining() {
        if (getTrackedTarget() == null) return 0;
        return (int) Math.max(0L, (trackedTargetExpiresAt - System.currentTimeMillis() + 999L) / 1000L);
    }

    private static void triggerSwordSwing() {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig config = VisualConfig.get();
        if (!config.swordAdjust || !config.swordSway || client.player == null) return;
        if (!(client.player.getMainHandStack().getItem() instanceof SwordItem)) return;
        lastSwordSwingAtNanos = System.nanoTime();
    }

    /** Normalized smooth pulse: 0 -> 1 -> 0. Triggered by each sword click only. */
    public static float getSwordSwingOffset() {
        VisualConfig config = VisualConfig.get();
        if (!config.swordAdjust || !config.swordSway || lastSwordSwingAtNanos == 0L) return 0.0f;

        long durationMs = Math.max(360L, 900L - config.swordSwaySpeed * 12L);
        double progress = (System.nanoTime() - lastSwordSwingAtNanos) / (durationMs * 1_000_000.0);
        if (progress >= 1.0 || progress < 0.0) return 0.0f;

        // Smooth attack pulse: ease into the leftward peak, then ease back to rest.
        double phase = progress < 0.42 ? progress / 0.42 : (1.0 - progress) / 0.58;
        phase = Math.max(0.0, Math.min(1.0, phase));
        return (float) (phase * phase * (3.0 - 2.0 * phase));
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
