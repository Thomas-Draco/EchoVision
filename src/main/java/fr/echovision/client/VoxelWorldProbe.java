package fr.echovision.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public final class VoxelWorldProbe implements VoxelShapeMesher.Outside {
    private final ClientWorld world;
    private final int originX, originY, originZ;
    private final Map<Integer,List<Box>> boxes = new HashMap<>();

    public VoxelWorldProbe(ClientWorld world, int x, int y, int z) {
        this.world=world;
        this.originX=x;
        this.originY=y;
        this.originZ=z;
    }

    @Override
    public boolean occupied(double x, double y, double z) {
        int dx=(int)Math.floor(x), dy=(int)Math.floor(y), dz=(int)Math.floor(z);
        if (dx < -1 || dx > 1 || dy < -1 || dy > 1 || dz < -1 || dz > 1) return false;
        if (dx == 0 && dy == 0 && dz == 0) return false;
        int key=(dx+1)*9+(dy+1)*3+(dz+1);
        List<Box> shapes=boxes.computeIfAbsent(key, k -> query(dx,dy,dz));
        double bx=x-dx, by=y-dy, bz=z-dz;
        for(Box box:shapes) if(box.contains(bx,by,bz)) return true;
        return false;
    }

    private List<Box> query(int dx, int dy, int dz) {
        int x=originX+dx, y=originY+dy, z=originZ+dz;
        if (!world.isChunkLoaded(x>>4,z>>4)
                || y < world.getBottomY() || y >= world.getTopY()) return List.of();
        BlockPos pos=new BlockPos(x,y,z);
        BlockState state=world.getBlockState(pos);
        return state.isAir() ? List.of() : state.getOutlineShape(world,pos).getBoundingBoxes();
    }
}
