package fr.echovision.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class NativeEntityModelEdges {
    private static final int MAX_CUBOIDS = 68;
    private static final int MAX_EDGES = 720;

    private NativeEntityModelEdges() {}

    // Cuboïdes natifs si accessibles ; fallback ailleurs.
    public static List<EchoSegment> extract(MinecraftClient client, Entity entity) {
        if (!(entity instanceof LivingEntity living) || client == null) return List.of();
        try {
            var renderer = client.getEntityRenderDispatcher().getRenderer(living);
            if (!(renderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) return List.of();
            EntityModel<?> model = livingRenderer.getModel();
            Iterable<ModelPart> roots;
            if (model instanceof SinglePartEntityModel<?> single) {
                roots = List.of(single.getPart());
            } else if (model instanceof CompositeEntityModel<?> composite) {
                roots = composite.getParts();
            } else {
                return List.of();
            }

            preparePose(model, living);
            ArrayList<Vec3d[]> raw = new ArrayList<>();
            MatrixStack matrices = new MatrixStack();
            for (ModelPart root : roots) {
                if (root == null) continue;
                root.forEachCuboid(matrices, (entry, name, index, cube) -> {
                    if (raw.size() >= MAX_CUBOIDS) return;
                    float[] xx = {cube.minX / 16.0F, cube.maxX / 16.0F};
                    float[] yy = {cube.minY / 16.0F, cube.maxY / 16.0F};
                    float[] zz = {cube.minZ / 16.0F, cube.maxZ / 16.0F};
                    Vec3d[] points = new Vec3d[8];
                    Matrix4f transform = entry.getPositionMatrix();
                    for (int i = 0; i < 8; i++) {
                        Vector3f p = new Vector3f(xx[i & 1], yy[(i >> 1) & 1], zz[(i >> 2) & 1]);
                        transform.transformPosition(p);
                        points[i] = new Vec3d(p.x(), p.y(), p.z());
                    }
                    raw.add(points);
                });
            }
            if (raw.isEmpty()) return List.of();

            double minX=Double.POSITIVE_INFINITY, minY=Double.POSITIVE_INFINITY, minZ=Double.POSITIVE_INFINITY;
            double maxX=Double.NEGATIVE_INFINITY, maxY=Double.NEGATIVE_INFINITY, maxZ=Double.NEGATIVE_INFINITY;
            for (Vec3d[] box : raw) for (Vec3d p : box) {
                minX=Math.min(minX,p.x); maxX=Math.max(maxX,p.x);
                minY=Math.min(minY,p.y); maxY=Math.max(maxY,p.y);
                minZ=Math.min(minZ,p.z); maxZ=Math.max(maxZ,p.z);
            }
            double nativeHeight=maxY-minY;
            double nativeWidth=Math.max(maxX-minX,maxZ-minZ);
            if (nativeHeight < .03 || nativeWidth < .03) return List.of();
            double sxz=entity.getWidth()/nativeWidth;
            double sy=entity.getHeight()/nativeHeight;

            double cx=(minX+maxX)*.5, cz=(minZ+maxZ)*.5;
            double theta=Math.toRadians(-living.getBodyYaw());
            double cos=Math.cos(theta), sin=Math.sin(theta);
            Vec3d world=entity.getPos();
            ArrayList<EchoSegment> output=new ArrayList<>(raw.size()*12);
            for (Vec3d[] box : raw) {
                Vec3d[] v=new Vec3d[8];
                for (int i=0;i<8;i++) {
                    Vec3d p=box[i];
                    double localX=(p.x-cx)*sxz, localZ=(p.z-cz)*sxz;
                    double localY=(maxY-p.y)*sy;
                    v[i]=world.add(localX*cos-localZ*sin, localY, localX*sin+localZ*cos);
                }
                for (int i=0;i<8 && output.size()<MAX_EDGES;i++) {
                    if ((i&1)==0) output.add(new EchoSegment(v[i],v[i|1]));
                    if ((i&2)==0) output.add(new EchoSegment(v[i],v[i|2]));
                    if ((i&4)==0) output.add(new EchoSegment(v[i],v[i|4]));
                }
            }
            return List.copyOf(output);
        } catch (RuntimeException ex) {

            return List.of();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void preparePose(EntityModel<?> model, LivingEntity entity) {

        EntityModel raw = model;
        float movement=(float)Math.min(1.0,entity.getVelocity().horizontalLength()*4.0);
        raw.animateModel(entity, entity.age * movement * .65F, movement, 0.0F);
        raw.setAngles(entity, entity.age * movement * .65F, movement, entity.age,
                entity.getHeadYaw()-entity.getBodyYaw(), entity.getPitch());
    }
}
