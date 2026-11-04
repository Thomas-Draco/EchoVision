package fr.echovision.client;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

public final class SculkSoundProfile {
    private SculkSoundProfile() {}

    public record Result(int frequency, int color) {}

    public static Result classify(SoundInstance sound, SoundCategory category, boolean selfEmitted) {
        String path = "";
        Identifier id = sound.getId();
        if (id != null) path = id.getPath();
        int frequency = classifyFrequency(path, category);
        return new Result(frequency, colorFor(frequency, selfEmitted));
    }

    public static int classifyFrequency(String path, SoundCategory category) {
        path = path == null ? "" : path;

        if (has(path, "explode", "explosion", "generic.explode", "lightning_bolt", "trident_thunder", "totem")) return 15;

        if (has(path, "teleport", "spawn", "summon", "entity_place", "evoker_prepare", "portal.travel")) return 14;

        if (has(path, ".place", "place", "block_place", "scaffold.place")) return 13;

        if (has(path, ".break", "break", "destroy", "extinguish", "snip", "shear")) return 12;

        if (has(path, "button", "lever", "trapdoor", "door", "fence_gate", "open", "close", "click", "pressure_plate", "tripwire", "comparator", "repeater", "piston", "note_block", "crop", "turtle_egg", "chiseled_bookshelf")) return 11;

        if (has(path, "chest", "barrel", "shulker", "ender_chest", "decorated_pot")) return 10;

        if (has(path, "armor", "equip", "bundle", "saddle")) return 9;

        if (has(path, "eat", "drink", "honey_drink", "burp")) return 8;

        if (has(path, "hurt", "damage", "burn", "freeze", "death", "attack.crit", "thorns")) return 7;

        if (has(path, "attack", "shoot", "mount", "dismount", "roar", "sonic_boom", "charge", "dash")) return 6;

        if (has(path, "prime", "prepare", "load", "crossbow", "wolf_shake", "sniffer", "camel", "iron_golem")) return 5;

        if (has(path, "elytra", "glide", "unequip", "brush", "spyglass")) return 4;

        if (has(path, "use", "throw", "fishing", "bone_meal", "bucket", "bottle", "item_pickup", "lodestone_compass_lock")) return 3;

        if (has(path, "land", "fall", "splash", "hit", "bounce", "projectile")) return 2;

        if (has(path, "step", "swim", "flap")) return 1;

        return switch (category) {
            case BLOCKS -> 11;
            case PLAYERS -> 1;
            case HOSTILE, NEUTRAL -> 6;
            case AMBIENT -> 4;
            default -> 5;
        };
    }

    public static int colorFor(int frequency, boolean selfEmitted) {
        int base = switch (Math.max(1, Math.min(15, frequency))) {
            case 1 -> 0x49D7F2;
            case 2 -> 0x5FD4B4;
            case 3 -> 0x6BEA8F;
            case 4 -> 0x7EEB6A;
            case 5 -> 0xB8E35C;
            case 6 -> 0xE1D95A;
            case 7 -> 0xF2B553;
            case 8 -> 0xF29A4E;
            case 9 -> 0xE98763;
            case 10 -> 0xD07FA5;
            case 11 -> 0xB47DDB;
            case 12 -> 0x8B85E6;
            case 13 -> 0x6C9AF0;
            case 14 -> 0x73C7F0;
            default -> 0x9BE9F1;
        };
        if (!selfEmitted) return base;
        return mixRgb(base, 0xA6F4FF, 0.42);
    }

    private static boolean has(String path, String... tokens) {
        for (String token : tokens) if (path.contains(token)) return true;
        return false;
    }

    private static int mixRgb(int a, int b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int)Math.round(ar + (br - ar) * t);
        int g = (int)Math.round(ag + (bg - ag) * t);
        int bl = (int)Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
