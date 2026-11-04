package fr.echovision.client;

public final class VoxelEdgeRules {
    private VoxelEdgeRules() {}

    public static boolean isFeatureEdge(boolean a, boolean b, boolean c, boolean d) {
        int count = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) + (d ? 1 : 0);
        return count == 1 || count == 3 || (count == 2 && ((a && d) || (b && c)));
    }

    public static double smoothstep(double a, double b, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - a) / (b - a)));
        return t * t * (3.0 - 2.0 * t);
    }
}
