package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.fold.NearestCopy;
import com.toroidalworld.engine.seam.MapSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CreateStationMapFold {
    public static BlockPos canonicalTarget(ResourceKey<Level> dimension, BlockPos target) {
        WorldFold transformer = transformerFor(dimension);
        return transformer == null ? target : canonicalTarget(transformer, target);
    }

    public static BlockPos canonicalTarget(WorldFold transformer, BlockPos target) {
        return transformer.fold(target);
    }

    public static BlockPos targetInMapFrame(ResourceKey<Level> dimension, int centreX, int centreZ, BlockPos target) {
        return targetInMapFrame(transformerFor(dimension), centreX, centreZ, target);
    }

    public static BlockPos targetInMapFrame(@Nullable WorldFold transformer, int centreX, int centreZ,
            BlockPos target) {
        return NearestCopy.toward(transformer, new BlockPos(centreX, target.getY(), centreZ), target);
    }

    public static Vec3 centreInMapFrame(ResourceKey<Level> dimension, int centreX, int centreZ, double x, double z) {
        return centreInMapFrame(transformerFor(dimension), centreX, centreZ, x, z);
    }

    public static Vec3 centreInMapFrame(@Nullable WorldFold transformer, int centreX, int centreZ, double x,
            double z) {
        return NearestCopy.toward(transformer, new Vec3(centreX, 0.0, centreZ), new Vec3(x, 0.0, z));
    }

    private static @Nullable WorldFold transformerFor(ResourceKey<Level> dimension) {
        return MapSeamFold.transformerFor(null, dimension);
    }

    private CreateStationMapFold() {
    }
}
