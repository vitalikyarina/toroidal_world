package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.map.MapSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class CreateStationMapFold {
    public static BlockPos canonicalTarget(ResourceKey<Level> dimension, BlockPos target) {
        WorldLoopTransformer transformer = transformerFor(dimension);
        return transformer == null ? target : transformer.blocks.wrap(target);
    }

    public static BlockPos targetInMapFrame(ResourceKey<Level> dimension, int centreX, int centreZ, BlockPos target) {
        WorldLoopTransformer transformer = transformerFor(dimension);
        if (transformer == null) {
            return target;
        }

        return new BlockPos(transformer.coords.x.unwrapAround(centreX, target.getX()), target.getY(),
                transformer.coords.z.unwrapAround(centreZ, target.getZ()));
    }

    public static double xInMapFrame(ResourceKey<Level> dimension, int centreX, double x) {
        WorldLoopTransformer transformer = transformerFor(dimension);
        return transformer == null ? x : transformer.coords.x.unwrapAround(centreX, x);
    }

    public static double zInMapFrame(ResourceKey<Level> dimension, int centreZ, double z) {
        WorldLoopTransformer transformer = transformerFor(dimension);
        return transformer == null ? z : transformer.coords.z.unwrapAround(centreZ, z);
    }

    private static @Nullable WorldLoopTransformer transformerFor(ResourceKey<Level> dimension) {
        return MapSeamFold.transformerFor(null, dimension);
    }

    private CreateStationMapFold() {
    }
}
