package fr.echovision.client;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class VoxelShapeMesher {
    private static final int MAX_AXES = 22;
    private static final int MAX_BOXES = 40;

    public record Mesh(List<EchoSegment> edges, List<EchoSurface> surfaces) {}

    @FunctionalInterface public interface Outside { boolean occupied(double x, double y, double z); }

    private VoxelShapeMesher() {}

    // Union des VoxelShape ; élimination des faces internes.
    public static Mesh mesh(VoxelShape shape, int bx, int by, int bz, int edgeLimit, int surfaceLimit) {
        return mesh(shape, bx, by, bz, edgeLimit, surfaceLimit, null);
    }

    public static Mesh mesh(VoxelShape shape, int bx, int by, int bz,
                            int edgeLimit, int surfaceLimit, Outside outside) {
        if (shape.isEmpty() || edgeLimit <= 0 && surfaceLimit <= 0) return new Mesh(List.of(), List.of());
        List<Box> boxes = shape.getBoundingBoxes();
        if (boxes.isEmpty() || boxes.size() > MAX_BOXES) return new Mesh(List.of(), List.of());
        TreeSet<Double> xCuts = new TreeSet<>(), yCuts = new TreeSet<>(), zCuts = new TreeSet<>();
        xCuts.add(0.0); xCuts.add(1.0);
        yCuts.add(0.0); yCuts.add(1.0);
        zCuts.add(0.0); zCuts.add(1.0);
        for (Box b : boxes) {
            xCuts.add(clamp(b.minX)); xCuts.add(clamp(b.maxX));
            yCuts.add(clamp(b.minY)); yCuts.add(clamp(b.maxY));
            zCuts.add(clamp(b.minZ)); zCuts.add(clamp(b.maxZ));
        }
        if (xCuts.size() > MAX_AXES || yCuts.size() > MAX_AXES || zCuts.size() > MAX_AXES)
            return new Mesh(List.of(), List.of());
        double[] xs = values(xCuts), ys = values(yCuts), zs = values(zCuts);
        int nx = xs.length - 1, ny = ys.length - 1, nz = zs.length - 1;
        boolean[][][] cells = new boolean[nx][ny][nz];
        for (int x = 0; x < nx; x++) for (int y = 0; y < ny; y++) for (int z = 0; z < nz; z++) {
            double xx = (xs[x] + xs[x + 1]) * .5;
            double yy = (ys[y] + ys[y + 1]) * .5;
            double zz = (zs[z] + zs[z + 1]) * .5;
            for (Box box : boxes) {
                if (box.contains(xx, yy, zz)) { cells[x][y][z] = true; break; }
            }
        }
        List<EchoSegment> edges = new ArrayList<>();
        if (edgeLimit > 0) {

            for (int y = 0; y <= ny && edges.size() < edgeLimit; y++) {
                for (int z = 0; z <= nz && edges.size() < edgeLimit; z++) {
                    int start = -1;
                    for (int x = 0; x <= nx; x++) {
                        boolean e = x < nx && VoxelEdgeRules.isFeatureEdge(
                                at(cells,xs,ys,zs,outside,x,y-1,z-1), at(cells,xs,ys,zs,outside,x,y,z-1),
                                at(cells,xs,ys,zs,outside,x,y-1,z), at(cells,xs,ys,zs,outside,x,y,z));
                        if (e && start < 0) start = x;
                        if (!e && start >= 0) {
                            edges.add(new EchoSegment(v(bx+xs[start], by+ys[y], bz+zs[z]),
                                    v(bx+xs[x], by+ys[y], bz+zs[z])));
                            start = -1;
                        }
                    }
                }
            }
            for (int x = 0; x <= nx && edges.size() < edgeLimit; x++) {
                for (int z = 0; z <= nz && edges.size() < edgeLimit; z++) {
                    int start = -1;
                    for (int y = 0; y <= ny; y++) {
                        boolean e = y < ny && VoxelEdgeRules.isFeatureEdge(
                                at(cells,xs,ys,zs,outside,x-1,y,z-1), at(cells,xs,ys,zs,outside,x,y,z-1),
                                at(cells,xs,ys,zs,outside,x-1,y,z), at(cells,xs,ys,zs,outside,x,y,z));
                        if (e && start < 0) start = y;
                        if (!e && start >= 0) {
                            edges.add(new EchoSegment(v(bx+xs[x], by+ys[start], bz+zs[z]),
                                    v(bx+xs[x], by+ys[y], bz+zs[z])));
                            start = -1;
                        }
                    }
                }
            }
            for (int x = 0; x <= nx && edges.size() < edgeLimit; x++) {
                for (int y = 0; y <= ny && edges.size() < edgeLimit; y++) {
                    int start = -1;
                    for (int z = 0; z <= nz; z++) {
                        boolean e = z < nz && VoxelEdgeRules.isFeatureEdge(
                                at(cells,xs,ys,zs,outside,x-1,y-1,z), at(cells,xs,ys,zs,outside,x,y-1,z),
                                at(cells,xs,ys,zs,outside,x-1,y,z), at(cells,xs,ys,zs,outside,x,y,z));
                        if (e && start < 0) start = z;
                        if (!e && start >= 0) {
                            edges.add(new EchoSegment(v(bx+xs[x], by+ys[y], bz+zs[start]),
                                    v(bx+xs[x], by+ys[y], bz+zs[z])));
                            start = -1;
                        }
                    }
                }
            }
        }
        List<EchoSurface> faces = new ArrayList<>();
        if (surfaceLimit > 0) {

            for (int axis = 0; axis < 3 && faces.size() < surfaceLimit; axis++) {
                int planes = axis == 0 ? nx : axis == 1 ? ny : nz;
                int uCount = axis == 0 ? ny : nx;
                int vCount = axis == 2 ? ny : nz;
                for (int plane = 0; plane <= planes && faces.size() < surfaceLimit; plane++) {
                    for (int side = 0; side < 2 && faces.size() < surfaceLimit; side++) {
                        boolean[][] mask = new boolean[uCount][vCount];
                        for (int u = 0; u < uCount; u++) for (int v = 0; v < vCount; v++) {
                            boolean left, right;
                            if (axis == 0) {
                                left = at(cells,xs,ys,zs,outside, plane-1, u, v); right = at(cells,xs,ys,zs,outside, plane, u, v);
                            } else if (axis == 1) {
                                left = at(cells,xs,ys,zs,outside, u, plane-1, v); right = at(cells,xs,ys,zs,outside, u, plane, v);
                            } else {
                                left = at(cells,xs,ys,zs,outside, u, v, plane-1); right = at(cells,xs,ys,zs,outside, u, v, plane);
                            }
                            mask[u][v] = side == 0 ? left && !right : right && !left;
                        }
                        for (int u = 0; u < uCount && faces.size() < surfaceLimit; u++) {
                            for (int vv = 0; vv < vCount && faces.size() < surfaceLimit; vv++) {
                                if (!mask[u][vv]) continue;
                                int endU = u + 1;
                                while (endU < uCount && mask[endU][vv]) endU++;
                                int endV = vv + 1;
                                scan: while (endV < vCount) {
                                    for (int t = u; t < endU; t++) if (!mask[t][endV]) break scan;
                                    endV++;
                                }
                                for (int i = u; i < endU; i++) for (int j = vv; j < endV; j++) mask[i][j] = false;
                                faces.add(face(axis, plane, u, endU, vv, endV, xs, ys, zs, bx, by, bz));
                            }
                        }
                    }
                }
            }
        }
        return new Mesh(List.copyOf(edges), List.copyOf(faces));
    }

    private static EchoSurface face(int axis, int plane, int u0, int u1, int v0, int v1,
                                    double[] xs, double[] ys, double[] zs, int bx, int by, int bz) {
        if (axis == 0) {
            double x=bx+xs[plane], y0=by+ys[u0], y1=by+ys[u1], z0=bz+zs[v0], z1=bz+zs[v1];
            return new EchoSurface(v(x,y0,z0),v(x,y1,z0),v(x,y1,z1),v(x,y0,z1));
        }
        if (axis == 1) {
            double y=by+ys[plane], x0=bx+xs[u0], x1=bx+xs[u1], z0=bz+zs[v0], z1=bz+zs[v1];
            return new EchoSurface(v(x0,y,z0),v(x1,y,z0),v(x1,y,z1),v(x0,y,z1));
        }
        double z=bz+zs[plane], x0=bx+xs[u0], x1=bx+xs[u1], y0=by+ys[v0], y1=by+ys[v1];
        return new EchoSurface(v(x0,y0,z),v(x1,y0,z),v(x1,y1,z),v(x0,y1,z));
    }

    private static boolean at(boolean[][][] grid, double[] xs, double[] ys, double[] zs,
                              Outside outside, int x, int y, int z) {
        if (x >= 0 && x < grid.length && y >= 0 && y < grid[0].length
                && z >= 0 && z < grid[0][0].length) return grid[x][y][z];
        if (outside == null) return false;
        return outside.occupied(sample(xs,x), sample(ys,y), sample(zs,z));
    }

    private static double sample(double[] cuts, int i) {
        if (i < 0) return cuts[0] - .0001;
        if (i >= cuts.length - 1) return cuts[cuts.length - 1] + .0001;
        return (cuts[i] + cuts[i+1]) * .5;
    }

    private static double[] values(TreeSet<Double> cuts) {
        return cuts.stream().mapToDouble(Double::doubleValue).toArray();
    }
    private static double clamp(double val) { return Math.max(0.0, Math.min(1.0, val)); }
    private static Vec3d v(double x,double y,double z) { return new Vec3d(x,y,z); }
}
