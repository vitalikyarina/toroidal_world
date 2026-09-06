package com.toroidalworld.core;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import net.minecraft.world.phys.Vec3;

public final class JomlVectors {
    private static final double HALF = 0.5;

    public static Vec3 read(Vector3dc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    public static Vec3 centre(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new Vec3((minX + maxX) * HALF, (minY + maxY) * HALF, (minZ + maxZ) * HALF);
    }

    public static Vector3d seated(Vector3d source, Vec3 raw, Vec3 seated) {
        return seated == raw ? source : new Vector3d(seated.x, seated.y, seated.z);
    }

    public static Vector3d seat(WorldFold fold, Vec3 anchor, Vector3d source) {
        Vec3 raw = read(source);
        return seated(source, raw, fold.nearestCopy(anchor, raw));
    }

    public static Vector3d write(Vec3 value, Vector3d target) {
        return target.set(value.x, value.y, value.z);
    }

    private JomlVectors() {
    }
}
