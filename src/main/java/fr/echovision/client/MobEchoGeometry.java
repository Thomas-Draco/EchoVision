package fr.echovision.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.Vec3d;

public final class MobEchoGeometry {
    private MobEchoGeometry() {}

    public static List<EchoSegment> fromEmitter(ClientWorld world, EchoPulse pulse) {
        if (world == null || pulse.sourceEntityId() < 0) return List.of();
        Entity entity = world.getEntityById(pulse.sourceEntityId());
        if (entity == null || entity.isRemoved()) return List.of();
        if (entity.getPos().squaredDistanceTo(pulse.origin()) > 24.0) return List.of();
        return fromEntity(entity);
    }

    public static List<EchoSegment> fromEntity(Entity entity) {
        if (entity == null || entity.isRemoved()) return List.of();
        double w = Math.max(.35, entity.getWidth());
        double h = Math.max(.45, entity.getHeight());
        double yaw = entity instanceof LivingEntity living ? living.getBodyYaw() : entity.getYaw();
        double theta = Math.toRadians(-yaw);
        double cos = Math.cos(theta), sin = Math.sin(theta);
        Vec3d origin = entity.getPos();
        Builder builder = new Builder(origin, cos, sin);

        EntityType<?> type = entity.getType();
        if (type == EntityType.ITEM || type == EntityType.EXPERIENCE_ORB) {
            box(builder, 0, h * .48, 0, w * .65, Math.max(.16, h * .56), w * .65);
        } else if (type == EntityType.BOAT || type == EntityType.CHEST_BOAT) {
            boat(builder, w, h);
        } else if (type == EntityType.MINECART || type == EntityType.CHEST_MINECART
                || type == EntityType.HOPPER_MINECART || type == EntityType.TNT_MINECART) {
            minecart(builder, w, h);
        } else if (type == EntityType.ARROW || type == EntityType.SPECTRAL_ARROW
                || type == EntityType.TRIDENT) {
            arrow(builder, w, h);
        } else if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER) {
            spider(builder, w, h);
        } else if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE) {
            box(builder, 0, h * .5, 0, w, h, w);
        } else if (type == EntityType.CHICKEN) {
            chicken(builder, w, h);
        } else if (type == EntityType.RABBIT) {
            rabbit(builder, w, h);
        } else if (type == EntityType.SQUID || type == EntityType.GLOW_SQUID) {
            squid(builder, w, h);
        } else if (type == EntityType.BLAZE) {
            blaze(builder, w, h);
        } else if (type == EntityType.GHAST) {
            ghast(builder, w, h);
        } else if (type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN) {
            guardian(builder, w, h);
        } else if (type == EntityType.VILLAGER || type == EntityType.WANDERING_TRADER
                || type == EntityType.WITCH) {
            villager(builder, w, h);
        } else if (type == EntityType.WOLF || type == EntityType.CAT || type == EntityType.OCELOT
                || type == EntityType.FOX) {
            pet(builder, w, h);
        } else if (type == EntityType.CREEPER) {
            creeper(builder, w, h);
        } else if (type == EntityType.SKELETON || type == EntityType.STRAY
                || type == EntityType.WITHER_SKELETON || type == EntityType.BOGGED) {
            lankyBiped(builder, w, h);
        } else if (type == EntityType.ENDERMAN) {
            lankyBiped(builder, w, h);
        } else if (type == EntityType.IRON_GOLEM || type == EntityType.WARDEN) {
            heavyBiped(builder, w, h);
        } else if (type == EntityType.BAT || type == EntityType.BEE || type == EntityType.ALLAY
                || type == EntityType.PARROT || type == EntityType.PHANTOM) {
            flyer(builder, w, h);
        } else if (entity instanceof AnimalEntity) {
            quadruped(builder, w, h);
        } else if (entity instanceof LivingEntity) {
            biped(builder, w, h);
        } else {
            box(builder, 0, h * .5, 0, w, h, w);
        }

        return builder.build();
    }

    private static void biped(Builder b, double w, double h) {
        double bodyW = w * .76, bodyD = w * .50;
        box(b, 0, h * .52, 0, bodyW, h * .43, bodyD);
        box(b, 0, h * .86, 0, w * .79, h * .22, w * .78);
        double limbW = w * .26;
        double legH = h * .37;
        box(b, -w * .20, legH * .52, 0, limbW, legH, bodyD * .85);
        box(b, w * .20, legH * .52, 0, limbW, legH, bodyD * .85);
        double armH = h * .38;
        box(b, -(bodyW * .50 + limbW * .50), h * .50, 0, limbW, armH, bodyD * .78);
        box(b, (bodyW * .50 + limbW * .50), h * .50, 0, limbW, armH, bodyD * .78);
    }

    private static void lankyBiped(Builder b, double w, double h) {
        double bodyW = w * .62, bodyD = w * .38;
        box(b, 0, h * .57, 0, bodyW, h * .40, bodyD);
        box(b, 0, h * .92, 0, w * .60, h * .18, w * .58);
        double limbW = w * .16;
        double legH = h * .53;
        box(b, -w * .15, legH * .50, 0, limbW, legH, bodyD * .65);
        box(b, w * .15, legH * .50, 0, limbW, legH, bodyD * .65);
        double armH = h * .58;
        box(b, -(bodyW * .55 + limbW * .55), h * .58, 0, limbW, armH, bodyD * .55);
        box(b, (bodyW * .55 + limbW * .55), h * .58, 0, limbW, armH, bodyD * .55);
    }

    private static void heavyBiped(Builder b, double w, double h) {
        double bodyW = w * .95, bodyD = w * .62;
        box(b, 0, h * .50, 0, bodyW, h * .48, bodyD);
        box(b, 0, h * .84, 0, w * .70, h * .24, w * .64);
        double limbW = w * .28;
        double legH = h * .38;
        box(b, -w * .18, legH * .52, 0, limbW, legH, bodyD * .75);
        box(b, w * .18, legH * .52, 0, limbW, legH, bodyD * .75);
        double armH = h * .44;
        box(b, -(bodyW * .56 + limbW * .54), h * .52, 0, limbW, armH, bodyD * .72);
        box(b, (bodyW * .56 + limbW * .54), h * .52, 0, limbW, armH, bodyD * .72);
    }

    private static void flyer(Builder b, double w, double h) {
        box(b, 0, h * .52, 0, w * .52, h * .38, w * .42);
        box(b, 0, h * .77, 0, w * .34, h * .18, w * .34);
        wing(b, -1, w, h);
        wing(b, 1, w, h);
    }

    private static void wing(Builder b, int side, double w, double h) {
        Vec3d shoulder = b.toWorld(side * w * .18, h * .58, 0);
        Vec3d mid = b.toWorld(side * w * .72, h * .70, w * .05);
        Vec3d tip = b.toWorld(side * w * 1.08, h * .54, w * .10);
        Vec3d low = b.toWorld(side * w * .76, h * .40, -w * .06);
        b.add(shoulder, mid);
        b.add(mid, tip);
        b.add(mid, low);
    }

    private static void creeper(Builder b, double w, double h) {
        box(b, 0, h * .53, 0, w * .70, h * .55, w * .60);
        box(b, 0, h * .89, 0, w * .83, h * .22, w * .83);
        for (int x = -1; x <= 1; x += 2) for (int z = -1; z <= 1; z += 2)
            box(b, x * w * .29, h * .125, z * w * .25, w * .28, h * .25, w * .29);
    }

    private static void quadruped(Builder b, double w, double h) {
        double bodyY = h * .58;
        double bodyLength = w * 1.25;
        box(b, 0, bodyY, 0, w * .78, h * .44, bodyLength);
        box(b, 0, h * .76, bodyLength * .43, w * .60, h * .40, w * .52);
        for (int x = -1; x <= 1; x += 2) for (int z = -1; z <= 1; z += 2)
            box(b, x * w * .26, h * .20, z * bodyLength * .33, w * .19, h * .40, w * .19);
    }

    private static void spider(Builder b, double w, double h) {
        box(b, 0, h * .47, -w * .22, w * .85, h * .49, w * .88);
        box(b, 0, h * .47, w * .47, w * .63, h * .40, w * .55);
        for (int i = 0; i < 4; i++) {
            double z = (i - 1.5) * w * .24;
            for (int side = -1; side <= 1; side += 2) {
                Vec3d shoulder = b.toWorld(side * w * .38, h * .52, z);
                Vec3d knee = b.toWorld(side * w * (0.68 + i * .04), h * .64, z + (i - 1.5) * w * .18);
                Vec3d foot = b.toWorld(side * w * 1.15, h * .04, z + (i - 1.5) * w * .32);
                b.add(shoulder, knee); b.add(knee, foot);
            }
        }
    }

    private static void villager(Builder b, double w, double h) {
        biped(b, w, h);
        box(b, 0, h * .52, 0, w * 1.04, h * .45, w * .86);
        box(b, 0, h * .84, w * .46, w * .16, h * .19, w * .17);
    }

    private static void pet(Builder b, double w, double h) {
        quadruped(b, w, h);
        for(int s=-1; s<=1; s+=2)
            box(b,s*w*.22,h*.95,w*.38,w*.16,h*.17,w*.15);
        b.add(b.toWorld(0,h*.60,-w*.69),b.toWorld(0,h*.81,-w*1.15));
    }

    private static void blaze(Builder b, double w, double h) {
        box(b,0,h*.67,0,w*.45,h*.40,w*.45);
        for(int i=0;i<12;i++) {
            double angle=i*Math.PI/6 + h*.07;
            double rr=w*(i%3==0 ? .58 : .39);
            double yy=h*(.23+(i%4)*.17);
            box(b,Math.cos(angle)*rr, yy, Math.sin(angle)*rr, w*.14,h*.24,w*.14);
        }
    }

    private static void ghast(Builder b, double w, double h) {
        box(b,0,h*.69,0,w,h*.59,w);
        for(int i=0;i<9;i++) {
            double px=(i%3-1)*w*.28, pz=(i/3-1)*w*.28;
            box(b,px,h*.22,pz,w*.09,h*.42,w*.09);
        }
    }

    private static void guardian(Builder b, double w, double h) {
        box(b,0,h*.5,0,w*.75,h*.72,w*.75);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            b.add(b.toWorld(Math.cos(a)*w*.34,h*.5,Math.sin(a)*w*.34),
                  b.toWorld(Math.cos(a)*w*.67,h*.58,Math.sin(a)*w*.67));
        }
        b.add(b.toWorld(0,h*.52,-w*.36),b.toWorld(0,h*.50,-w*1.05));
    }

    private static void boat(Builder b, double w, double h) {
        box(b, 0, h * .24, 0, w, h * .22, w * 1.7);
        box(b, -w * .43, h * .46, 0, w * .14, h * .42, w * 1.7);
        box(b, w * .43, h * .46, 0, w * .14, h * .42, w * 1.7);
        box(b, 0, h * .46, -w * .79, w, h * .42, w * .15);
        box(b, 0, h * .46, w * .79, w, h * .42, w * .15);
    }

    private static void minecart(Builder b, double w, double h) {
        box(b, 0, h * .22, 0, w, h * .20, w);
        box(b, -w * .46, h * .52, 0, w * .09, h * .55, w);
        box(b, w * .46, h * .52, 0, w * .09, h * .55, w);
        box(b, 0, h * .52, -w * .46, w, h * .55, w * .09);
        box(b, 0, h * .52, w * .46, w, h * .55, w * .09);
    }

    private static void arrow(Builder b, double w, double h) {
        double y = Math.max(.12, h * .5);
        b.add(b.toWorld(0, y, -Math.max(w, .6)), b.toWorld(0, y, Math.max(w, .6)));
        b.add(b.toWorld(-.13, y, -.37), b.toWorld(.13, y, .37));
        b.add(b.toWorld(.13, y, -.37), b.toWorld(-.13, y, .37));
    }

    private static void chicken(Builder b, double w, double h) {
        box(b, 0, h*.54, -w*.08, w*.9, h*.48, w*.78);
        box(b, 0, h*.84, w*.32, w*.48, h*.30, w*.44);
        box(b, 0, h*.77, w*.58, w*.20, h*.13, w*.28);
        for(int s=-1;s<=1;s+=2) box(b, s*w*.18,h*.16,0,w*.07,h*.28,w*.08);
    }

    private static void rabbit(Builder b, double w, double h) {
        box(b, 0, h*.35, -w*.12, w*.88, h*.47, w*.95);
        box(b, 0, h*.55, w*.34, w*.5, h*.40, w*.50);
        for(int s=-1;s<=1;s+=2) box(b, s*w*.20,h*.83,w*.39,w*.13,h*.30,w*.13);
    }

    private static void squid(Builder b, double w, double h) {
        box(b, 0, h*.75, 0, w*.82, h*.42, w*.82);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            b.add(b.toWorld(Math.cos(a)*w*.25,h*.55,Math.sin(a)*w*.25),
                    b.toWorld(Math.cos(a)*w*.47,h*.05,Math.sin(a)*w*.47));
        }
    }

    private static void box(Builder builder, double x, double y, double z, double w, double h, double d) {
        double minX = x - w / 2, maxX = x + w / 2;
        double minY = y - h / 2, maxY = y + h / 2;
        double minZ = z - d / 2, maxZ = z + d / 2;
        Vec3d[] v = new Vec3d[8];
        for (int i = 0; i < 8; i++)
            v[i] = builder.toWorld((i & 1) == 0 ? minX : maxX,
                    (i & 2) == 0 ? minY : maxY, (i & 4) == 0 ? minZ : maxZ);
        for (int i = 0; i < 8; i++) {
            if ((i & 1) == 0) builder.add(v[i], v[i | 1]);
            if ((i & 2) == 0) builder.add(v[i], v[i | 2]);
            if ((i & 4) == 0) builder.add(v[i], v[i | 4]);
        }
    }

    private static final class Builder {
        private final Vec3d origin;
        private final double cos, sin;
        private final List<EchoSegment> segments = new ArrayList<>();
        private Builder(Vec3d origin, double cos, double sin) {
            this.origin = origin; this.cos = cos; this.sin = sin;
        }
        private Vec3d toWorld(double x, double y, double z) {
            return origin.add(x * cos - z * sin, y, x * sin + z * cos);
        }
        private void add(Vec3d a, Vec3d b) { segments.add(new EchoSegment(a, b)); }
        private List<EchoSegment> build() { return List.copyOf(segments); }
    }
}
