package fr.echovision.client;

public final class VoxelEdgeRulesSelfTest {
    public static void main(String[] args) {
        int count = 0;
        for (int mask = 0; mask < 16; mask++) {
            boolean a = (mask & 1) != 0;
            boolean b = (mask & 2) != 0;
            boolean c = (mask & 4) != 0;
            boolean d = (mask & 8) != 0;
            int bits = Integer.bitCount(mask);
            boolean expected = bits == 1 || bits == 3 || mask == 9 || mask == 6;
            if (VoxelEdgeRules.isFeatureEdge(a, b, c, d) != expected) {
                throw new AssertionError("Lattice mask " + mask);
            }
            count++;
        }
        if (VoxelEdgeRules.smoothstep(0, 1, -.5) != 0) throw new AssertionError("start");
        if (VoxelEdgeRules.smoothstep(0, 1, 1.5) != 1) throw new AssertionError("end");
        if (Math.abs(VoxelEdgeRules.smoothstep(0, 1, .5) - .5) > 1e-9)
            throw new AssertionError("mid");
        check(countOuterEdges(1, 1, 1) == 12, "cube unitaire : 12 arêtes");
        check(countOuterEdges(4, 4, 1) == 36, "mur 4x4 : aucun quadrillage interne");
        check(countOuterEdges(5, 5, 1) == 44, "mur 5x5 : 44 segments extérieurs");
        check(countOuterEdges(2, 2, 2) == 24, "cube 2x2x2 : arêtes externes uniquement");
        System.out.println("VoxelEdgeRules: " + (count+7) + " tests OK");
    }
    private static int countOuterEdges(int sx, int sy, int sz) {
        int total=0;
        for(int x=0;x<=sx;x++) for(int y=0;y<=sy;y++) for(int z=0;z<=sz;z++) {
            if(x<sx && VoxelEdgeRules.isFeatureEdge(
                inside(x,y-1,z-1,sx,sy,sz),inside(x,y,z-1,sx,sy,sz),
                inside(x,y-1,z,sx,sy,sz),inside(x,y,z,sx,sy,sz))) total++;
            if(y<sy && VoxelEdgeRules.isFeatureEdge(
                inside(x-1,y,z-1,sx,sy,sz),inside(x,y,z-1,sx,sy,sz),
                inside(x-1,y,z,sx,sy,sz),inside(x,y,z,sx,sy,sz))) total++;
            if(z<sz && VoxelEdgeRules.isFeatureEdge(
                inside(x-1,y-1,z,sx,sy,sz),inside(x,y-1,z,sx,sy,sz),
                inside(x-1,y,z,sx,sy,sz),inside(x,y,z,sx,sy,sz))) total++;
        }
        return total;
    }

    private static boolean inside(int x,int y,int z,int sx,int sy,int sz) {
        return x>=0 && y>=0 && z>=0 && x<sx && y<sy && z<sz;
    }

    private static void check(boolean ok, String test) {
        if (!ok) throw new AssertionError(test);
    }

}
