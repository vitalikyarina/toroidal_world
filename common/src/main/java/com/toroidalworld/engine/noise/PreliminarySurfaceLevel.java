package com.toroidalworld.engine.noise;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

// On 1.21.1 the router carries the pre-jaggedness density where 26.x carries a find_top_surface node, so the column
// search NoiseChunk does there is done here instead.
public record PreliminarySurfaceLevel(DensityFunction density) implements DensityFunction {
    private static final double SOLID_THRESHOLD = 0.390625;

    private static final int SEARCH_FLOOR = -64;

    private static final int SEARCH_HEIGHT = 384;

    private static final int SEARCH_STEP = 8;

    @Override
    public double compute(FunctionContext context) {
        int blockX = context.blockX();
        int blockZ = context.blockZ();

        for (int y = SEARCH_FLOOR + SEARCH_HEIGHT; y >= SEARCH_FLOOR; y -= SEARCH_STEP) {
            if (this.density.compute(new SinglePointContext(blockX, y, blockZ)) > SOLID_THRESHOLD) {
                return y;
            }
        }

        return SEARCH_FLOOR;
    }

    @Override
    public void fillArray(double[] values, ContextProvider provider) {
        provider.fillAllDirectly(values, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new PreliminarySurfaceLevel(this.density.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return SEARCH_FLOOR;
    }

    @Override
    public double maxValue() {
        return SEARCH_FLOOR + SEARCH_HEIGHT;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        throw new UnsupportedOperationException("Calling .codec() on PreliminarySurfaceLevel");
    }
}
