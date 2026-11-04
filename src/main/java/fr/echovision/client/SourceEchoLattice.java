package fr.echovision.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class SourceEchoLattice {
    private static final int RADIUS_XZ = 13;
    private static final int RADIUS_Y = 10;
    private static final int MAX_SOURCE_EDGES = 4400;
    private static final int MAX_SOURCE_SHAPES = 150;
    private static final int CACHE_LIMIT = 9;
    private static final int MAX_SURFACES = 2800;
    private final MinecraftClient client;
    private ClientWorld cachedWorld;
    private final Map<Long, SourceGeometry> cache = new LinkedHashMap<>();
    private final Map<BlockPos, RecentGeometry> byLocation = new LinkedHashMap<>();
    private record RecentGeometry(long tick, SourceGeometry geometry) {}

    public record SourceGeometry(List<EchoSegment> edges, List<EchoSurface> surfaces) {}

    public SourceEchoLattice(MinecraftClient client) { this.client = client; }

    public void tick(boolean active) {
        if (!active || client.world != cachedWorld) {
            cache.clear();
            byLocation.clear();
            cachedWorld = active ? client.world : null;
        }
    }

    // Scan étalé : une nouvelle source par tick.
    public void prewarmOne(List<EchoPulse> pulses) {
        ClientWorld world = client.world;
        if (world == null || world != cachedWorld) return;
        for (int i = pulses.size() - 1; i >= 0; i--) {
            EchoPulse pulse = pulses.get(i);
            if (cache.containsKey(pulse.bornNanos())) continue;
            if (cache.size() >= CACHE_LIMIT) {
                Iterator<Long> it = cache.keySet().iterator();
                if (it.hasNext()) { it.next(); it.remove(); }
            }
            BlockPos cell = BlockPos.ofFloored(pulse.origin());
            long tick = world.getTime();
            RecentGeometry recent = byLocation.get(cell);
            SourceGeometry geometry;
            if (recent != null && tick - recent.tick() >= 0 && tick - recent.tick() < 35) {
                geometry = recent.geometry();
            } else {
                geometry = build(world, pulse.origin());
                if (byLocation.size() >= 12) {
                    Iterator<BlockPos> old = byLocation.keySet().iterator();
                    if (old.hasNext()) { old.next(); old.remove(); }
                }
                byLocation.put(cell, new RecentGeometry(tick, geometry));
            }
            cache.put(pulse.bornNanos(), geometry);
            break;
        }
    }

    public SourceGeometry around(EchoPulse pulse) {
        SourceGeometry prepared = cache.get(pulse.bornNanos());
        return prepared == null ? new SourceGeometry(List.of(), List.of()) : prepared;
    }

    private static SourceGeometry build(ClientWorld world, Vec3d source) {
        BlockPos center = BlockPos.ofFloored(source);
        final int nx = 2 * RADIUS_XZ + 4;
        final int ny = 2 * RADIUS_Y + 4;
        final int nz = nx;
        final int ox = center.getX() - RADIUS_XZ - 2;
        final int oy = center.getY() - RADIUS_Y - 2;
        final int oz = center.getZ() - RADIUS_XZ - 2;
        byte[] solid = new byte[nx * ny * nz];
        ArrayList<EchoSegment> details = new ArrayList<>();
        ArrayList<EchoSurface> surfaces = new ArrayList<>();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        ArrayList<BlockPos> nonCubes = new ArrayList<>();

        for (int x = 0; x < nx; x++) {
            int wx = ox + x;
            for (int z = 0; z < nz; z++) {
                int wz = oz + z;
                if (!world.isChunkLoaded(wx >> 4, wz >> 4)) continue;
                for (int y = 0; y < ny; y++) {
                    int wy = oy + y;
                    if (wy < world.getBottomY() || wy >= world.getTopY()) continue;
                    mutable.set(wx, wy, wz);
                    BlockState state = world.getBlockState(mutable);
                    if (state.isAir()) continue;
                    if (state.isFullCube(world, mutable)) {
                        solid[(x * ny + y) * nz + z] = 1;
                    } else if (x >= 2 && x < nx - 2 && y >= 2 && y < ny - 2 && z >= 2 && z < nz - 2
                            && source.squaredDistanceTo(new Vec3d(wx + 0.5, wy + 0.5, wz + 0.5)) < 196) {
                        nonCubes.add(mutable.toImmutable());
                    }
                }
            }
        }

        nonCubes.sort(java.util.Comparator.comparingDouble(p -> p.getSquaredDistance(center)));
        for (int i = 0; i < Math.min(MAX_SOURCE_SHAPES, nonCubes.size()); i++) {
            BlockPos p = nonCubes.get(i);
            var shape = world.getBlockState(p).getOutlineShape(world, p);
            var mesh = VoxelShapeMesher.mesh(shape, p.getX(), p.getY(), p.getZ(),
                    Math.min(180, MAX_SOURCE_EDGES - details.size()),
                    Math.min(90, MAX_SURFACES - surfaces.size()),
                    new VoxelWorldProbe(world, p.getX(), p.getY(), p.getZ()));
            details.addAll(mesh.edges());
            surfaces.addAll(mesh.surfaces());
            if (details.size() >= MAX_SOURCE_EDGES || surfaces.size() >= MAX_SURFACES) break;
        }

        ArrayList<EchoSegment> edges = new ArrayList<>(Math.min(MAX_SOURCE_EDGES, details.size() + 2000));

        for (int x = 2; x < nx - 2 && edges.size() < MAX_SOURCE_EDGES; x++) {
            for (int y = 2; y < ny - 2 && edges.size() < MAX_SOURCE_EDGES; y++) {
                for (int z = 2; z < nz - 2 && edges.size() < MAX_SOURCE_EDGES; z++) {
                    int wx = ox + x, wy = oy + y, wz = oz + z;
                    if (source.squaredDistanceTo(new Vec3d(wx, wy, wz)) > 15.0 * 15.0) continue;
                    if (VoxelEdgeRules.isFeatureEdge(
                            occupied(solid, x, y - 1, z - 1, ny, nz), occupied(solid, x, y, z - 1, ny, nz),
                            occupied(solid, x, y - 1, z, ny, nz), occupied(solid, x, y, z, ny, nz))) {
                        edges.add(new EchoSegment(new Vec3d(wx, wy, wz), new Vec3d(wx + 1, wy, wz)));
                    }
                    if (VoxelEdgeRules.isFeatureEdge(
                            occupied(solid, x - 1, y, z - 1, ny, nz), occupied(solid, x, y, z - 1, ny, nz),
                            occupied(solid, x - 1, y, z, ny, nz), occupied(solid, x, y, z, ny, nz))) {
                        edges.add(new EchoSegment(new Vec3d(wx, wy, wz), new Vec3d(wx, wy + 1, wz)));
                    }
                    if (VoxelEdgeRules.isFeatureEdge(
                            occupied(solid, x - 1, y - 1, z, ny, nz), occupied(solid, x, y - 1, z, ny, nz),
                            occupied(solid, x - 1, y, z, ny, nz), occupied(solid, x, y, z, ny, nz))) {
                        edges.add(new EchoSegment(new Vec3d(wx, wy, wz), new Vec3d(wx, wy, wz + 1)));
                    }
                }
            }
        }

        for (int x = 2; x < nx - 2 && surfaces.size() < MAX_SURFACES; x++) {
            for (int y = 2; y < ny - 2 && surfaces.size() < MAX_SURFACES; y++) {
                for (int z = 2; z < nz - 2 && surfaces.size() < MAX_SURFACES; z++) {
                    if (!occupied(solid, x, y, z, ny, nz)) continue;
                    int wx = ox + x, wy = oy + y, wz = oz + z;
                    if (source.squaredDistanceTo(new Vec3d(wx + .5, wy + .5, wz + .5)) > 12 * 12) continue;
                    if (!occupied(solid, x, y + 1, z, ny, nz)) addSurface(surfaces, wx, wy, wz, 0);
                    if (!occupied(solid, x, y - 1, z, ny, nz)) addSurface(surfaces, wx, wy, wz, 1);
                    if (!occupied(solid, x, y, z - 1, ny, nz)) addSurface(surfaces, wx, wy, wz, 2);
                    if (!occupied(solid, x, y, z + 1, ny, nz)) addSurface(surfaces, wx, wy, wz, 3);
                    if (!occupied(solid, x - 1, y, z, ny, nz)) addSurface(surfaces, wx, wy, wz, 4);
                    if (!occupied(solid, x + 1, y, z, ny, nz)) addSurface(surfaces, wx, wy, wz, 5);
                }
            }
        }
        for (EchoSegment seg : details) {
            if (edges.size() >= MAX_SOURCE_EDGES) break;
            if (seg.center().squaredDistanceTo(source) < 13.5 * 13.5) edges.add(seg);
        }
        return new SourceGeometry(List.copyOf(edges), List.copyOf(surfaces));
    }

    private static void addSurface(List<EchoSurface> list, double x, double y, double z, int face) {
        if (list.size() >= MAX_SURFACES) return;
        Vec3d a, b, c, d;
        switch (face) {
            case 0 -> {
                a = v(x, y+1, z); b = v(x+1, y+1, z);
                c = v(x+1, y+1, z+1); d = v(x, y+1, z+1);
            }
            case 1 -> {
                a = v(x, y, z); b = v(x+1, y, z);
                c = v(x+1, y, z+1); d = v(x, y, z+1);
            }
            case 2 -> {
                a = v(x, y, z); b = v(x+1, y, z);
                c = v(x+1, y+1, z); d = v(x, y+1, z);
            }
            case 3 -> {
                a = v(x, y, z+1); b = v(x+1, y, z+1);
                c = v(x+1, y+1, z+1); d = v(x, y+1, z+1);
            }
            case 4 -> {
                a = v(x, y, z); b = v(x, y, z+1);
                c = v(x, y+1, z+1); d = v(x, y+1, z);
            }
            default -> {
                a = v(x+1, y, z); b = v(x+1, y, z+1);
                c = v(x+1, y+1, z+1); d = v(x+1, y+1, z);
            }
        }
        list.add(new EchoSurface(a, b, c, d));
    }

    private static Vec3d v(double x, double y, double z) { return new Vec3d(x, y, z); }

    private static boolean occupied(byte[] b, int x, int y, int z, int ny, int nz) {
        return b[(x * ny + y) * nz + z] != 0;
    }
}
