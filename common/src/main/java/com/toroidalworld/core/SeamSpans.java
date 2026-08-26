package com.toroidalworld.core;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class SeamSpans {
    public static boolean crossesSeam(WorldFold fold, BoundingBox region) {
        return fold.blockDomain(Direction.Axis.X).spansSeam(region.minX(), region.maxX())
                || fold.blockDomain(Direction.Axis.Z).spansSeam(region.minZ(), region.maxZ());
    }

    public static BoundingBox foldAcrossSeam(WorldFold fold, BoundingBox region) {
        if (!crossesSeam(fold, region)) {
            return region;
        }

        WrapDomain x = fold.blockDomain(Direction.Axis.X);
        WrapDomain z = fold.blockDomain(Direction.Axis.Z);
        return new BoundingBox(
                x.foldSpanStart(region.minX(), region.maxX()),
                region.minY(),
                z.foldSpanStart(region.minZ(), region.maxZ()),
                x.foldSpanEnd(region.minX(), region.maxX()),
                region.maxY(),
                z.foldSpanEnd(region.minZ(), region.maxZ()));
    }

    private SeamSpans() {
    }
}
