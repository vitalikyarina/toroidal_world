package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.map.MapSeamFold;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class CreateStationMapFold {
    public static BlockPos canonicalTarget(ResourceKey<Level> dimension, BlockPos target) {
        WorldFold transformer = transformerFor(dimension);
        return transformer == null ? target : transformer.fold(target);
    }

    public static BlockPos targetInMapFrame(ResourceKey<Level> dimension, int centreX, int centreZ, BlockPos target) {
        WorldFold transformer = transformerFor(dimension);
        if (transformer == null) {
            return target;
        }

        return new BlockPos(transformer.blockDomain(Direction.Axis.X).unwrapAround(centreX, target.getX()), target.getY(),
                transformer.blockDomain(Direction.Axis.Z).unwrapAround(centreZ, target.getZ()));
    }

    public static double xInMapFrame(ResourceKey<Level> dimension, int centreX, double x) {
        WorldFold transformer = transformerFor(dimension);
        return transformer == null ? x : transformer.blockDomain(Direction.Axis.X).unwrapAround(centreX, x);
    }

    public static double zInMapFrame(ResourceKey<Level> dimension, int centreZ, double z) {
        WorldFold transformer = transformerFor(dimension);
        return transformer == null ? z : transformer.blockDomain(Direction.Axis.Z).unwrapAround(centreZ, z);
    }

    private static @Nullable WorldFold transformerFor(ResourceKey<Level> dimension) {
        return MapSeamFold.transformerFor(null, dimension);
    }

    private CreateStationMapFold() {
    }
}
