package fr.echovision.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

public final class EchoSystem implements SoundInstanceListener {
    private static final int MAX_PULSES = 8;
    private static final double MAX_HEARING = 56.0;
    private final MinecraftClient client;
    private final Deque<EchoPulse> pulses = new ArrayDeque<>();
    private ClientWorld previousWorld;
    private boolean previouslyActive;
    private long nextFootstepTick;
    private final long[] lastByCategory = new long[SoundCategory.values().length];

    public EchoSystem(MinecraftClient client) { this.client = client; }

    @Override
    // Sons filtrés par catégorie, puis impulsions locales.
    public void onSoundPlayed(SoundInstance sound, WeightedSoundSet soundSet, float range) {
        if (sound.isRelative()) return;
        SoundCategory cat = sound.getCategory();
        if (cat == SoundCategory.MUSIC || cat == SoundCategory.RECORDS || cat == SoundCategory.MASTER
                || cat == SoundCategory.VOICE || cat == SoundCategory.WEATHER) return;
        Vec3d source = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
        if (!Double.isFinite(source.x) || !Double.isFinite(source.y) || !Double.isFinite(source.z)) return;
        float volume = sound.getVolume();
        if (volume < .024F && cat != SoundCategory.HOSTILE && cat != SoundCategory.PLAYERS) return;
        client.execute(() -> {
            if (!isActive() || source.squaredDistanceTo(client.player.getEyePos()) > MAX_HEARING * MAX_HEARING) return;
            long now = System.nanoTime();
            int key = cat.ordinal();
            long interval = switch (cat) {
                case HOSTILE -> 62_000_000L;
                case PLAYERS -> 75_000_000L;
                case BLOCKS -> 110_000_000L;
                case NEUTRAL -> 160_000_000L;
                case AMBIENT -> 570_000_000L;
                default -> 230_000_000L;
            };
            if (now - lastByCategory[key] < interval) return;
            lastByCategory[key] = now;

            int entityId = identifyEmitter(source, cat);
            boolean self = isSelfEmitted(source, cat, entityId);
            SculkSoundProfile.Result profile = SculkSoundProfile.classify(sound, cat, self);
            double force = Math.max(.3, Math.min(1.0, .30 + Math.sqrt(Math.max(0.0, volume)) * .55));
            addPulse(source, profile.color(), profile.frequency(), self, force, false, entityId);
        });
    }

    public void tick() {
        if (previousWorld != client.world) {
            pulses.clear();
            previousWorld = client.world;
            previouslyActive = false;
            nextFootstepTick = 0;
            Arrays.fill(lastByCategory, 0L);
        }
        boolean active = isActive();
        if (!active) {
            pulses.clear();
            previouslyActive = false;
            return;
        }
        if (!previouslyActive) addPulse(client.player.getEyePos(), 0x92F0FF, 1, true, .86, true, client.player.getId());
        previouslyActive = true;
        long now = System.nanoTime();
        pulses.removeIf(p -> p.isExpired(now));
        long tick = client.world.getTime();
        double speed2 = client.player.getVelocity().horizontalLengthSquared();
        if (tick >= nextFootstepTick && client.player.isOnGround()
                && !client.player.hasVehicle() && speed2 > 0.0035) {
            int color = SculkSoundProfile.colorFor(1, true);
            addPulse(client.player.getPos().add(0, 0.24, 0), color, 1, true, .55, true, client.player.getId());
            nextFootstepTick = tick + (speed2 > .05 ? 6 : 9);
        }
    }

    public boolean isActive() {
        return client.world != null && client.player != null && EchoVisionState.isEnabled();
    }

    public List<EchoPulse> snapshot() { return List.copyOf(pulses); }

    private int identifyEmitter(Vec3d soundPos, SoundCategory category) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) return -1;
        if (category != SoundCategory.HOSTILE && category != SoundCategory.NEUTRAL
                && category != SoundCategory.PLAYERS) return -1;

        LivingEntity bestEntity = null;
        double best = 2.9 * 2.9;
        if (soundPos.squaredDistanceTo(client.player.getBoundingBox().getCenter()) < best) {
            best = soundPos.squaredDistanceTo(client.player.getBoundingBox().getCenter());
            bestEntity = client.player;
        }
        for (Entity e : world.getOtherEntities(client.player, new Box(soundPos, soundPos).expand(2.9),
                e -> e instanceof LivingEntity && e.isAlive())) {
            double d = soundPos.squaredDistanceTo(e.getBoundingBox().getCenter());
            if (d < best) {
                best = d;
                bestEntity = (LivingEntity)e;
            }
        }
        return bestEntity == null ? -1 : bestEntity.getId();
    }

    private boolean isSelfEmitted(Vec3d soundPos, SoundCategory category, int entityId) {
        if (client.player == null) return false;
        if (entityId == client.player.getId()) return true;
        if (category == SoundCategory.PLAYERS) {
            return soundPos.squaredDistanceTo(client.player.getEyePos()) < 2.2 * 2.2;
        }
        return false;
    }

    private void addPulse(Vec3d origin, int color, int frequency, boolean selfEmitted,
                          double force, boolean local, int entityId) {
        double r = local ? 22.0 : Math.min(36.0, 22.0 + force * 14.0);
        pulses.addLast(new EchoPulse(origin, color, frequency, selfEmitted, force, System.nanoTime(),
                local ? 10.8 : 15.0, r, entityId));
        while (pulses.size() > MAX_PULSES) pulses.removeFirst();
    }
}
