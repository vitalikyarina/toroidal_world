package com.toroidalworld.noise;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class QuantizedCoordinates {
    public static DensityFunction.FunctionContext inBlocks(DensityFunction.FunctionContext cell, int cellWidth) {
        return new DensityFunction.SinglePointContext(
                cell.blockX() * cellWidth, cell.blockY(), cell.blockZ() * cellWidth);
    }

    private QuantizedCoordinates() {
    }
}
