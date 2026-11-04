package fr.echovision.client;

import net.minecraft.util.math.Vec3d;

public record EchoPulse(Vec3d origin, int color, int frequency, boolean selfEmitted,
                        double strength, long bornNanos,
                        double speed, double maxRadius, int sourceEntityId) {
    public double age(long nanos) { return Math.max(0, (nanos - bornNanos) * 1.0e-9); }
    public double radius(long nanos) { return Math.min(maxRadius, age(nanos) * speed); }
    public boolean isExpired(long nanos) { return age(nanos) > maxRadius / speed + 1.55; }
}
