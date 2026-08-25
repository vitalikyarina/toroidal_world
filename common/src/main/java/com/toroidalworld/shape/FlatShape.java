package com.toroidalworld.shape;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

public record FlatShape(WorldLoopBounds bounds, int skewChunks, @Nullable Mirror mirror) {

    public record Mirror(Direction.Axis axis, int lineChunk) {
        public Mirror {
            if (axis == Direction.Axis.Y) {
                throw new IllegalArgumentException("A flat identification never mirrors Y");
            }
        }
    }

    public enum Identification {
        RECTANGLE,
        CYLINDER,
        MOBIUS,
        KLEIN,
        LATTICE_TORUS
    }

    public FlatShape {
        boolean xLooped = bounds.x() instanceof AxisBounds.Looped;
        boolean zLooped = bounds.z() instanceof AxisBounds.Looped;

        if (skewChunks != 0 && !(xLooped && zLooped)) {
            throw new IllegalArgumentException("A skewed lattice needs both axes looped, got " + bounds);
        }

        if (mirror != null) {
            if (skewChunks != 0) {
                throw new IllegalArgumentException("A skewed mirror is not one of the five flat identifications");
            }

            boolean glideAxisLooped = mirror.axis() == Direction.Axis.X ? zLooped : xLooped;
            if (!glideAxisLooped) {
                throw new IllegalArgumentException("A mirror on " + mirror.axis()
                        + " needs the axis it glides along to loop, got " + bounds);
            }
        }

        if (skewChunks != 0) {
            skewChunks = normalizeSkew(skewChunks, ((AxisBounds.Looped) bounds.x()).chunkWidth());
        }
    }

    public static FlatShape rectangle() {
        return new FlatShape(WorldLoopBounds.UNBOUNDED, 0, null);
    }

    public static FlatShape cylinder(WorldLoopBounds bounds) {
        return new FlatShape(bounds, 0, null);
    }

    public static FlatShape latticeTorus(WorldLoopBounds bounds, int skewChunks) {
        return new FlatShape(bounds, skewChunks, null);
    }

    public static FlatShape mirrored(WorldLoopBounds bounds, Direction.Axis mirroredAxis, int mirrorLineChunk) {
        return new FlatShape(bounds, 0, new Mirror(mirroredAxis, mirrorLineChunk));
    }

    public Identification identification() {
        boolean xLooped = bounds.x() instanceof AxisBounds.Looped;
        boolean zLooped = bounds.z() instanceof AxisBounds.Looped;
        if (!xLooped && !zLooped) {
            return Identification.RECTANGLE;
        }

        if (xLooped && zLooped) {
            return mirror != null ? Identification.KLEIN : Identification.LATTICE_TORUS;
        }

        return mirror != null ? Identification.MOBIUS : Identification.CYLINDER;
    }

    public boolean isMirrored() {
        return mirror != null;
    }

    public boolean decomposesPerAxis() {
        return mirror == null && skewChunks == 0;
    }

    public boolean preservesLocalIndices() {
        return mirror == null;
    }

    private static int normalizeSkew(int skewChunks, int xChunkWidth) {
        int remainder = Math.floorMod(skewChunks, xChunkWidth);
        return 2 * remainder > xChunkWidth ? remainder - xChunkWidth : remainder;
    }
}
