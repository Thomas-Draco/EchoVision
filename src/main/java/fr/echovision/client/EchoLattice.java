package fr.echovision.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class EchoLattice {
    private static final int RX = 21;
    private static final int RY = 13;
    private static final int RZ = 21;
    private static final int MAX_SEGMENTS = 8400;
    private static final int DETAIL_RADIUS = 11;
    private static final int REBUILD_TICKS = 14;

    private final MinecraftClient client;
    private ClientWorld cachedWorld;
    private BlockPos cachedCenter;
    private long builtAt = Long.MIN_VALUE;
    private List<EchoSegment> edges = List.of();

    public EchoLattice(MinecraftClient client) { this.client = client; }

    public List<EchoSegment> snapshot() { return edges; }

    public void reset() {
        cachedWorld = null;
        cachedCenter = null;
        builtAt = Long.MIN_VALUE;
        edges = List.of();
    }

    public void tick(boolean wearing) {
        if (!wearing || client.world == null || client.player == null) {
            if (!edges.isEmpty()) reset();
            return;
        }
        ClientWorld world = client.world;
        BlockPos center = client.player.getBlockPos();
        long tick = world.getTime();
        if (world == cachedWorld && cachedCenter != null
                && center.getSquaredDistance(cachedCenter) < 4.0
                && tick - builtAt < REBUILD_TICKS) return;

        cachedWorld = world;
        cachedCenter = center;
        builtAt = tick;
        rebuild(world, center);
    }

    // Maillage local : arêtes réelles, sans joints coplanaires.
    private void rebuild(ClientWorld world, BlockPos c) {
        int nx = RX * 2 + 4, ny = RY * 2 + 4, nz = RZ * 2 + 4;
        int ox = c.getX() - RX - 2, oy = c.getY() - RY - 2, oz = c.getZ() - RZ - 2;
        byte[] solid = new byte[nx * ny * nz];
        List<EchoSegment> result = new ArrayList<>();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        List<BlockPos> details = new ArrayList<>();

        for (int x = 0; x < nx; x++) {
            for (int z = 0; z < nz; z++) {
                int wx = ox + x, wz = oz + z;
                if (!world.isChunkLoaded(wx >> 4, wz >> 4)) continue;
                for (int y = 0; y < ny; y++) {
                    int wy = oy + y;
                    if (wy < world.getBottomY() || wy >= world.getTopY()) continue;
                    mutable.set(wx, wy, wz);
                    BlockState state = world.getBlockState(mutable);
                    if (state.isAir()) continue;

                    if (state.isFullCube(world, mutable)) {
                        solid[index(x, y, z, ny, nz)] = 1;
                    } else if (x > 1 && x < nx - 2 && y > 1 && y < ny - 2 && z > 1 && z < nz - 2
                            && Math.abs(wx - c.getX()) <= DETAIL_RADIUS
                            && Math.abs(wy - c.getY()) <= DETAIL_RADIUS
                            && Math.abs(wz - c.getZ()) <= DETAIL_RADIUS) {
                        details.add(mutable.toImmutable());
                    }
                }
            }
        }

        details.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(c)));
        for (int i = 0; i < Math.min(190, details.size()); i++) {
            BlockPos pos = details.get(i);
            appendNonCubic(result, world.getBlockState(pos).getOutlineShape(world, pos),
                    pos.getX(), pos.getY(), pos.getZ(), world);
        }

        for (int x = 2; x < nx - 2; x++) {
            for (int y = 2; y < ny - 2; y++) {
                for (int z = 2; z < nz - 2; z++) {
                    int wx = ox + x, wy = oy + y, wz = oz + z;
                    if (VoxelEdgeRules.isFeatureEdge(
                            occupied(solid, x, y - 1, z - 1, ny, nz),
                            occupied(solid, x, y, z - 1, ny, nz),
                            occupied(solid, x, y - 1, z, ny, nz),
                            occupied(solid, x, y, z, ny, nz))) {
                        result.add(new EchoSegment(new Vec3d(wx, wy, wz), new Vec3d(wx + 1, wy, wz)));
                    }
                    if (VoxelEdgeRules.isFeatureEdge(
                            occupied(solid, x - 1, y, z - 1, ny, nz),
                            occupied(solid, x, y, z - 1, ny, nz),
                            occupied(solid, x - 1, y, z, ny, nz),
                            occupied(solid, x, y, z, ny, nz))) {
                        result.add(new EchoSegment(new Vec3d(wx, wy, wz), new Vec3d(wx, wy + 1, wz)));
                    }
                    if (VoxelEdgeRules.isFeatureEdge(
                            occupied(solid, x - 1, y - 1, z, ny, nz),
                            occupied(solid, x, y - 1, z, ny, nz),
                            occupied(solid, x - 1, y, z, ny, nz),
                            occupied(solid, x, y, z, ny, nz))) {
                        result.add(new EchoSegment(new Vec3d(wx, wy, wz), new Vec3d(wx, wy, wz + 1)));
                    }
                }
            }
        }

        Vec3d eye = client.player.getEyePos();
        result.removeIf(edge -> edge.center().squaredDistanceTo(eye) > (RX * RX + 3.0)
                || !lineOfSight(solid, nx, ny, nz, ox, oy, oz, eye, edge.center()));
        if (result.size() > MAX_SEGMENTS) {
            result.sort(Comparator.comparingDouble(edge -> edge.center().squaredDistanceTo(eye)));
            result = new ArrayList<>(result.subList(0, MAX_SEGMENTS));
        }
        edges = List.copyOf(result);
    }

    static void appendNonCubic(List<EchoSegment> out, VoxelShape shape, int x, int y, int z) {
        if (out.size() >= MAX_SEGMENTS) return;
        var geometry = VoxelShapeMesher.mesh(shape, x, y, z,
                Math.min(180, MAX_SEGMENTS - out.size()), 0);
        out.addAll(geometry.edges());
    }

    static void appendNonCubic(List<EchoSegment> out, VoxelShape shape, int x, int y, int z,
                               ClientWorld world) {
        if (out.size() >= MAX_SEGMENTS) return;
        var geometry = VoxelShapeMesher.mesh(shape, x, y, z,
                Math.min(180, MAX_SEGMENTS - out.size()), 0,
                new VoxelWorldProbe(world, x, y, z));
        out.addAll(geometry.edges());
    }

    static void appendNonCubicSurfaces(List<EchoSurface> out, VoxelShape shape,
                                       int x, int y, int z, int maxSurfacesToAdd) {
        if (maxSurfacesToAdd <= 0) return;
        var geometry = VoxelShapeMesher.mesh(shape, x, y, z, 0,
                Math.min(90, maxSurfacesToAdd));
        out.addAll(geometry.surfaces());
    }

    private static boolean lineOfSight(byte[] solid, int nx, int ny, int nz,
                                       int ox, int oy, int oz, Vec3d eye, Vec3d target) {
        Vec3d delta = target.subtract(eye);
        double distance = delta.length();
        if (distance <= 1.1) return true;
        Vec3d dir = delta.multiply(1.0 / distance);

        for (double travel = .5; travel < distance - .85; travel += .55) {
            int x = (int)Math.floor(eye.x + dir.x * travel) - ox;
            int y = (int)Math.floor(eye.y + dir.y * travel) - oy;
            int z = (int)Math.floor(eye.z + dir.z * travel) - oz;
            if (x >= 0 && y >= 0 && z >= 0 && x < nx && y < ny && z < nz
                    && occupied(solid, x, y, z, ny, nz)) return false;
        }
        return true;
    }

    private static int index(int x, int y, int z, int ny, int nz) {
        return (x * ny + y) * nz + z;
    }

    private static boolean occupied(byte[] grid, int x, int y, int z, int ny, int nz) {
        return grid[index(x, y, z, ny, nz)] != 0;
    }
}
