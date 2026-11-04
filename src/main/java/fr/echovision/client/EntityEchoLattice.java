package fr.echovision.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class EntityEchoLattice {
    private static final int MAX_ENTITIES = 28;
    private static final double OBSERVER_RADIUS = 38.0;
    private final MinecraftClient client;
    private ClientWorld world;
    private long builtTick = Long.MIN_VALUE;
    private List<Trace> traces = List.of();

    public record Trace(int entityId, Vec3d center, List<EchoSegment> edges) {}

    public EntityEchoLattice(MinecraftClient client) { this.client = client; }

    public List<Trace> snapshot() { return traces; }

    public void tick(boolean active, List<EchoPulse> pulses) {
        if (!active || client.world == null || client.player == null) {
            traces = List.of(); world = null; builtTick = Long.MIN_VALUE;
            return;
        }
        ClientWorld current = client.world;
        long tick = current.getTime();
        if (current == world && tick >= builtTick && tick - builtTick < 2) return;
        world = current;
        builtTick = tick;

        Set<Entity> candidates = new LinkedHashSet<>();
        Vec3d observer = client.player.getEyePos();
        collect(current, new Box(observer, observer).expand(OBSERVER_RADIUS), candidates);

        for (EchoPulse pulse : pulses) {
            if (pulse.origin().squaredDistanceTo(observer) > 25.0 * 25.0) {
                collect(current, new Box(pulse.origin(), pulse.origin()).expand(13.0), candidates);
            }
            if (pulse.sourceEntityId() >= 0) {
                Entity source = current.getEntityById(pulse.sourceEntityId());
                if (source != null && source != client.player && eligible(source)) candidates.add(source);
            }
        }
        List<Entity> sorted = new ArrayList<>(candidates);

        Set<Integer> emitters = new java.util.HashSet<>();
        for (EchoPulse pulse : pulses) if (pulse.sourceEntityId() >= 0) emitters.add(pulse.sourceEntityId());
        sorted.sort(Comparator.comparingInt((Entity e) -> emitters.contains(e.getId()) ? 0 : 1)
                .thenComparingDouble(e -> e.getPos().squaredDistanceTo(observer)));
        ArrayList<Trace> output = new ArrayList<>();
        for (Entity entity : sorted) {
            if (output.size() >= MAX_ENTITIES) break;
            if (entity.isRemoved()) continue;
            List<EchoSegment> segments = NativeEntityModelEdges.extract(client, entity);
            if (segments.isEmpty()) segments = MobEchoGeometry.fromEntity(entity);
            if (!segments.isEmpty()) output.add(new Trace(entity.getId(), entity.getBoundingBox().getCenter(), segments));
        }
        traces = List.copyOf(output);
    }

    private void collect(ClientWorld current, Box box, Set<Entity> out) {
        out.addAll(current.getOtherEntities(client.player, box, EntityEchoLattice::eligible));
    }

    private static boolean eligible(Entity entity) {
        if (entity.isRemoved()) return false;
        if (entity instanceof LivingEntity) return true;
        EntityType<?> type = entity.getType();
        return type == EntityType.ITEM || type == EntityType.EXPERIENCE_ORB
                || type == EntityType.BOAT || type == EntityType.CHEST_BOAT
                || type == EntityType.MINECART || type == EntityType.CHEST_MINECART
                || type == EntityType.HOPPER_MINECART || type == EntityType.TNT_MINECART
                || type == EntityType.ARROW || type == EntityType.SPECTRAL_ARROW
                || type == EntityType.TRIDENT;
    }
}
