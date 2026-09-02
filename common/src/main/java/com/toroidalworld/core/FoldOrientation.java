package com.toroidalworld.core;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public enum FoldOrientation {
    IDENTITY(false, false),
    MIRROR_X(true, false),
    MIRROR_Z(false, true),
    HALF_TURN(true, true);

    private static final FoldOrientation[] BY_FLIPS = {IDENTITY, MIRROR_Z, MIRROR_X, HALF_TURN};

    private static final double BLOCK_WIDTH = 1.0;

    private final boolean flipsX;
    private final boolean flipsZ;

    FoldOrientation(boolean flipsX, boolean flipsZ) {
        this.flipsX = flipsX;
        this.flipsZ = flipsZ;
    }

    public static FoldOrientation of(boolean flipsX, boolean flipsZ) {
        return BY_FLIPS[(flipsX ? 2 : 0) | (flipsZ ? 1 : 0)];
    }

    public boolean flipsX() {
        return this.flipsX;
    }

    public boolean flipsZ() {
        return this.flipsZ;
    }

    public boolean isIdentity() {
        return this == IDENTITY;
    }

    public boolean preservesHandedness() {
        return this.flipsX == this.flipsZ;
    }

    public int signX() {
        return this.flipsX ? -1 : 1;
    }

    public int signZ() {
        return this.flipsZ ? -1 : 1;
    }

    public FoldOrientation compose(FoldOrientation next) {
        return of(this.flipsX ^ next.flipsX, this.flipsZ ^ next.flipsZ);
    }

    public Vec3 applyToDelta(Vec3 delta) {
        if (isIdentity()) {
            return delta;
        }

        return new Vec3(this.flipsX ? -delta.x : delta.x, delta.y, this.flipsZ ? -delta.z : delta.z);
    }

    public Vec3 applyToBlockOffset(Vec3 offset) {
        if (isIdentity()) {
            return offset;
        }

        return new Vec3(
                this.flipsX ? BLOCK_WIDTH - offset.x : offset.x,
                offset.y,
                this.flipsZ ? BLOCK_WIDTH - offset.z : offset.z);
    }

    public Direction applyToFace(Direction face) {
        if (isIdentity()) {
            return face;
        }

        return switch (face.getAxis()) {
            case X -> this.flipsX ? face.getOpposite() : face;
            case Y -> face;
            case Z -> this.flipsZ ? face.getOpposite() : face;
        };
    }
}
