package com.toroidalworld.core;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class SeamDelta {
    public static double foldX(WorldFold fold, double deltaX) {
        return fold.blockDomain(Direction.Axis.X).unwrapAround(0.0, deltaX);
    }

    public static double foldZ(WorldFold fold, double deltaZ) {
        return fold.blockDomain(Direction.Axis.Z).unwrapAround(0.0, deltaZ);
    }

    public static Vec3 fold(WorldFold fold, Vec3 delta) {
        double foldedX = foldX(fold, delta.x);
        double foldedZ = foldZ(fold, delta.z);
        return foldedX == delta.x && foldedZ == delta.z ? delta : new Vec3(foldedX, delta.y, foldedZ);
    }

    private SeamDelta() {
    }
}
