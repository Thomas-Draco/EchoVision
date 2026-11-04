package fr.echovision.client;

import net.minecraft.util.math.Vec3d;

public record EchoSegment(Vec3d a, Vec3d b) {
    public Vec3d center() { return a.add(b).multiply(0.5); }
}
