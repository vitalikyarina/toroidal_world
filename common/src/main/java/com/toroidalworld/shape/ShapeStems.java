package com.toroidalworld.shape;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.shape.LoopSpans;
import com.toroidalworld.api.v1.shape.ShapeDimensions;
import com.toroidalworld.core.NetherScales;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class ShapeStems {

    public static @Nullable LoopSpans spansOf(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            Predicate<LoopSpans> ownGeometry) {
        LoopSpans spans = ShapeDimensions.spansOf(dimensions, key);
        return spans != null && ownGeometry.test(spans) ? spans : null;
    }

    public static LoopSpans netherSpans(LoopSpans overworld, int netherScale, int chunkWidth) {
        return overworld.scaledDown(NetherScales.normalize(netherScale, chunkWidth));
    }

    public static int readNetherScale(WorldDimensions dimensions, Predicate<LoopSpans> ownGeometry,
            Direction.Axis axis, int overworldChunkWidth) {
        LoopSpans nether = spansOf(dimensions, LevelStem.NETHER, ownGeometry);
        return nether != null && nether.loops(axis)
                ? overworldChunkWidth / nether.chunkWidth(axis)
                : NetherScales.DEFAULT;
    }

    private ShapeStems() {
    }
}
