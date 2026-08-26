package com.toroidalworld.noise;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext.Context;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class CanonicalCellSampler {
    public interface Sample {
        double at(DensityFunction.FunctionContext cell);
    }

    private final int vanillaCellWidth;

    private @Nullable TilingCellGrid grid;

    public CanonicalCellSampler(int vanillaCellWidth) {
        this.vanillaCellWidth = vanillaCellWidth;
    }

    public double sample(DensityFunction.FunctionContext cell, int blockX, int blockZ, Sample sample) {
        Context generation = GenerationTransformerContext.context();
        WorldFold transformer = generation.wrappedTransformer();
        if (transformer == null) {
            return sample.at(cell);
        }

        TilingCellGrid grid = TilingCellGrid.resolve(this.grid, transformer, this.vanillaCellWidth);
        this.grid = grid;

        try (Context.DivisorScope divisorScope = generation.withDivisors(grid.xCellWidth(), grid.zCellWidth())) {
            return sample.at(QuantizedCoordinates.inBlocks(grid, blockX, cell.blockY(), blockZ));
        }
    }
}
