package com.toroidalworld.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class SeamDelta {
    public static double foldX(WorldFold fold, double deltaX) {
        return fold.blockDomain(Direction.Axis.X).unwrapAround(0.0, deltaX);
    }

    public static double foldZ(WorldFold fold, double deltaZ) {
        return fold.blockDomain(Direction.Axis.Z).unwrapAround(0.0, deltaZ);
    }

    public static int foldX(WorldFold fold, int deltaX) {
        return fold.blockDomain(Direction.Axis.X).unwrapAround(0, deltaX);
    }

    public static int foldZ(WorldFold fold, int deltaZ) {
        return fold.blockDomain(Direction.Axis.Z).unwrapAround(0, deltaZ);
    }

    public static Vec3 fold(WorldFold fold, Vec3 delta) {
        double foldedX = foldX(fold, delta.x);
        double foldedZ = foldZ(fold, delta.z);
        return foldedX == delta.x && foldedZ == delta.z ? delta : new Vec3(foldedX, delta.y, foldedZ);
    }

    public static BlockPos fold(WorldFold fold, BlockPos delta) {
        int foldedX = foldX(fold, delta.getX());
        int foldedZ = foldZ(fold, delta.getZ());
        return foldedX == delta.getX() && foldedZ == delta.getZ()
                ? delta
                : new BlockPos(foldedX, delta.getY(), foldedZ);
    }

    private SeamDelta() {
    }
}
