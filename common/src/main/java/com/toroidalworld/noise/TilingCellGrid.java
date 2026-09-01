package com.toroidalworld.noise;

import org.jspecify.annotations.Nullable;

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
        if (domain instanceof WrapDomain.Noop) {
            return vanillaCellWidth;
        }

        int width = domain.domainLength;
        double vanillaCellCount = (double) width / vanillaCellWidth;
        int cellCount = SMALLEST_CELL_COUNT;

        for (int candidate = 1; (long) candidate * candidate <= width; candidate++) {
            if (width % candidate != 0) {
                continue;
            }

            cellCount = nearer(cellCount, candidate, vanillaCellCount);
            cellCount = nearer(cellCount, width / candidate, vanillaCellCount);
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
