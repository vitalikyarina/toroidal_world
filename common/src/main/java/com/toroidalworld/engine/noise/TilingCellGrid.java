package com.toroidalworld.engine.noise;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.Divisors;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.core.Direction;

public record TilingCellGrid(WorldFold transformer, int xCellWidth, int zCellWidth) {
    private static final int SMALLEST_CELL_COUNT = 1;

    public static TilingCellGrid of(WorldFold transformer, int vanillaCellWidth) {
        return new TilingCellGrid(transformer,
                tilingWidth(transformer.blockDomain(Direction.Axis.X), vanillaCellWidth),
                tilingWidth(transformer.blockDomain(Direction.Axis.Z), vanillaCellWidth));
    }

    public static TilingCellGrid resolve(@Nullable TilingCellGrid cached,
            WorldFold transformer, int vanillaCellWidth) {
        return cached != null && cached.transformer == transformer
                ? cached
                : of(transformer, vanillaCellWidth);
    }

    public int cellOriginX(int blockX) {
        return Math.floorDiv(blockX, this.xCellWidth) * this.xCellWidth;
    }

    public int cellOriginZ(int blockZ) {
        return Math.floorDiv(blockZ, this.zCellWidth) * this.zCellWidth;
    }

    static int tilingWidth(WrapDomain domain, int vanillaCellWidth) {
        if (!domain.loops()) {
            return vanillaCellWidth;
        }

        int width = domain.domainLength;
        double vanillaCellCount = (double) width / vanillaCellWidth;
        int cellCount = SMALLEST_CELL_COUNT;

        for (int divisor : Divisors.of(width)) {
            cellCount = nearer(cellCount, divisor, vanillaCellCount);
        }

        return width / cellCount;
    }

    private static int nearer(int chosen, int candidate, double vanillaCellCount) {
        double chosenDistance = Math.abs(chosen - vanillaCellCount);
        double candidateDistance = Math.abs(candidate - vanillaCellCount);
        if (candidateDistance < chosenDistance) {
            return candidate;
        }

        return candidateDistance > chosenDistance ? chosen : Math.max(chosen, candidate);
    }
}
