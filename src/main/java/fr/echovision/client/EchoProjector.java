package fr.echovision.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;

public final class EchoProjector {
    private Matrix4f projection = new Matrix4f();
    private Quaternionf inverseRotation = new Quaternionf();
    private Vec3d cameraPos = Vec3d.ZERO;
    private boolean captured;

    public void capture(WorldRenderContext context) {
        cameraPos = context.camera().getPos();
        inverseRotation = new Quaternionf(context.camera().getRotation()).conjugate();
        projection = new Matrix4f(context.projectionMatrix());
        captured = true;
    }

    public ProjectedPoint project(Vec3d world, int width, int height) {
        if (!captured) return null;
        Vector3f view = new Vector3f((float) (world.x - cameraPos.x),
                (float) (world.y - cameraPos.y), (float) (world.z - cameraPos.z));
        view.rotate(inverseRotation);
        Vector4f clip = new Vector4f(view.x, view.y, view.z, 1).mul(projection);
        if (clip.w <= 0.07F) return null;
        float nx = clip.x / clip.w, ny = clip.y / clip.w;
        if (!Float.isFinite(nx) || !Float.isFinite(ny) || Math.abs(nx) > 3.0F || Math.abs(ny) > 3.0F) return null;
        return new ProjectedPoint((nx * .5F + .5F) * width,
                (-ny * .5F + .5F) * height,
                view.length());
    }

    public ProjectedPoint bearing(Vec3d world, int width, int height) {
        if (!captured) return null;
        Vector3f view = new Vector3f((float) (world.x - cameraPos.x),
                (float) (world.y - cameraPos.y), (float) (world.z - cameraPos.z));
        view.rotate(inverseRotation);
        double angle = Math.atan2(view.x, -view.z);
        float dx = (float) Math.sin(angle);
        float dy = (float) -Math.cos(angle);
        float xr = Math.max(12, width * .5F - 21F);
        float yr = Math.max(12, height * .5F - 28F);
        float scale = Math.min(xr / Math.max(.05F, Math.abs(dx)),
                yr / Math.max(.05F, Math.abs(dy)));
        return new ProjectedPoint(width * .5F + dx * scale,
                height * .5F + dy * scale, view.length());
    }

    public record ProjectedPoint(float x, float y, float distance) {}
}
