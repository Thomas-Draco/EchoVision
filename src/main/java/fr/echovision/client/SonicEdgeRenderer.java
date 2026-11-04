package fr.echovision.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

import fr.echovision.client.EchoProjector.ProjectedPoint;

public final class SonicEdgeRenderer {
    private static volatile ShaderProgram shader;
    private static final float RIBBON_HALF_WIDTH_GUI = 4.2F;
    private static final int MAX_RENDER_SEGMENTS = 4000;
    private final EchoSystem echoes;
    private final EchoLattice lattice;
    private final SourceEchoLattice sourceLattice;
    private final EntityEchoLattice entityLattice;
    private final EchoProjector projector;
    private final MinecraftClient client;
    private final java.util.Map<Integer, Boolean> entityVisibility = new java.util.HashMap<>();
    private net.minecraft.client.world.ClientWorld visibilityWorld;
    private long visibilityTick = Long.MIN_VALUE;

    public SonicEdgeRenderer(EchoSystem echoes, EchoLattice lattice,
                            SourceEchoLattice sourceLattice, EntityEchoLattice entityLattice,
                            EchoProjector projector, MinecraftClient client) {
        this.echoes = echoes;
        this.lattice = lattice;
        this.sourceLattice = sourceLattice;
        this.entityLattice = entityLattice;
        this.projector = projector;
        this.client = client;
    }

    public static void setShader(ShaderProgram program) { shader = program; }

    // Arêtes et crêtes projetées ; illumination GLSL.
    public void render(DrawContext context) {
        if (!echoes.isActive() || client.currentScreen != null) return;
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();

        context.fill(0, 0, w, h, 0xFF000306);
        ShaderProgram effect = shader;
        if (effect == null) return;
        List<EchoSegment> observerSegments = lattice.snapshot();
        List<EchoPulse> pulses = echoes.snapshot();
        if (pulses.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        try {
            RenderSystem.setShader(() -> effect);
            long now = System.nanoTime();
            float pixels = (float) client.getWindow().getScaleFactor() * 3.0F;
            Matrix4f gui = context.getMatrices().peek().getPositionMatrix();
            for (EchoPulse pulse : pulses) {
                double age = pulse.age(now);
                double wave = pulse.radius(now);
                double life = pulse.maxRadius() / pulse.speed() + 1.55;
                double fade = Math.max(0.0, Math.min(1.0, (life - age) * .85));
                if (fade < .025) continue;
                float strength = (float) (pulse.strength() * fade);
                setUniform(effect, "WaveRadius", (float) wave);
                setUniform(effect, "WaveAge", (float) age);
                setUniform(effect, "WaveStrength", strength);
                setUniform(effect, "PixelSize", Math.max(3.0F, pixels));
                setUniform(effect, "WaveColor", ((pulse.color() >> 16) & 255) / 255.0F,
                        ((pulse.color() >> 8) & 255) / 255.0F, (pulse.color() & 255) / 255.0F);

                SourceEchoLattice.SourceGeometry sourceGeo = sourceLattice.around(pulse);
                setUniform(effect, "SurfaceMode", 1.0F);
                renderSurfaces(gui, w, h, sourceGeo.surfaces(), pulse, wave);
                setUniform(effect, "SurfaceMode", 0.0F);
                renderPulse(gui, w, h, sourceGeo.edges(), pulse, wave, false);

                renderPulse(gui, w, h, observerSegments, pulse, wave, true);

                for (EntityEchoLattice.Trace trace : entityLattice.snapshot()) {
                    double range = trace.center().distanceTo(pulse.origin());
                    if (range > pulse.maxRadius() + 2 || range > wave + 3 || range < wave - 9) continue;
                    if (!visibleEntity(trace)) continue;
                    renderPulse(gui, w, h, trace.edges(), pulse, wave, false);
                }
            }
        } finally {

            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }
    }

    private boolean visibleEntity(EntityEchoLattice.Trace trace) {
        if (client.player == null || client.world == null) return false;
        long tick = client.world.getTime();
        if (visibilityWorld != client.world || visibilityTick != tick) {
            entityVisibility.clear();
            visibilityWorld = client.world;
            visibilityTick = tick;
        }
        return entityVisibility.computeIfAbsent(trace.entityId(), id -> hasLineOfSight(trace.center()));
    }

    private boolean hasLineOfSight(Vec3d center) {
        Vec3d eye = client.player.getEyePos();
        if (eye.squaredDistanceTo(center) < 2.25) return true;

        var hit = client.world.raycast(new net.minecraft.world.RaycastContext(
                eye, center, net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE, client.player));
        return hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS
                || hit.getPos().squaredDistanceTo(center) < 0.40;
    }

    private void renderSurfaces(Matrix4f gui, int w, int h, List<EchoSurface> surfaces,
                                EchoPulse pulse, double wave) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);
        Vec3d origin = pulse.origin();
        int count = 0;
        for (EchoSurface surface : surfaces) {
            if (count >= 1600) break;
            Vec3d[] corners = {surface.a(), surface.b(), surface.c(), surface.d()};
            double[] distances = new double[4];
            double near = Double.MAX_VALUE, far = 0;
            for (int j = 0; j < 4; j++) {
                distances[j] = corners[j].distanceTo(origin);
                near = Math.min(near, distances[j]);
                far = Math.max(far, distances[j]);
            }

            if (near > wave + .9 || far < wave - .85 || near > pulse.maxRadius() + 1) continue;
            ProjectedPoint[] projected = new ProjectedPoint[4];
            boolean ok = true;
            for (int j = 0; j < 4; j++) {
                projected[j] = projector.project(corners[j], w, h);
                if (projected[j] == null) { ok = false; break; }
            }
            if (!ok) continue;
            for (int j = 0; j < 4; j++) {
                vertex(buffer, gui, projected[j].x(), projected[j].y(), (float) distances[j], 0.0F);
            }
            count++;
        }
        var built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private void renderPulse(Matrix4f gui, int w, int h, List<EchoSegment> segments,
                             EchoPulse pulse, double wave, boolean skipSourceOverlap) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);
        Vec3d origin = pulse.origin();
        int count = 0;
        for (EchoSegment seg : segments) {
            if (count >= MAX_RENDER_SEGMENTS) break;
            if (skipSourceOverlap && seg.center().squaredDistanceTo(origin) < 12.5 * 12.5) continue;
            double d0 = origin.distanceTo(seg.a());
            double d1 = origin.distanceTo(seg.b());
            double near = Math.min(d0, d1);
            double far = Math.max(d0, d1);

            if (near > wave + 1.5 || far < wave - 7.0) continue;
            if (near > pulse.maxRadius() + 1.5) continue;

            ProjectedPoint a = projector.project(seg.a(), w, h);
            ProjectedPoint b = projector.project(seg.b(), w, h);
            if (a == null || b == null) continue;
            float dx = b.x() - a.x();
            float dy = b.y() - a.y();
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.65F || len > 2200.0F) continue;
            float halfWidth = RIBBON_HALF_WIDTH_GUI;
            float nx = -dy / len * halfWidth;
            float ny = dx / len * halfWidth;
            if (Math.max(a.x(), b.x()) < -halfWidth || Math.min(a.x(), b.x()) > w + halfWidth
                    || Math.max(a.y(), b.y()) < -halfWidth || Math.min(a.y(), b.y()) > h + halfWidth) continue;

            vertex(buffer, gui, a.x() + nx, a.y() + ny, (float) d0, 1.0F);
            vertex(buffer, gui, b.x() + nx, b.y() + ny, (float) d1, 1.0F);
            vertex(buffer, gui, b.x() - nx, b.y() - ny, (float) d1, -1.0F);
            vertex(buffer, gui, a.x() - nx, a.y() - ny, (float) d0, -1.0F);
            count++;
        }

        var built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f gui, float x, float y, float distance, float side) {
        buffer.vertex(gui, x, y, 0.0F).texture(distance, side).color(255, 255, 255, 255);
    }

    private static void setUniform(ShaderProgram shader, String name, float value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void setUniform(ShaderProgram shader, String name, float x, float y, float z) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y, z);
    }
}
