package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.map.MapSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CreateStationMapFold {
    public static BlockPos canonicalTarget(ResourceKey<Level> dimension, BlockPos target) {
        WorldFold transformer = transformerFor(dimension);
        return transformer == null ? target : transformer.fold(target);
    }

    public static BlockPos targetInMapFrame(ResourceKey<Level> dimension, int centreX, int centreZ, BlockPos target) {
        WorldFold transformer = transformerFor(dimension);
        return transformer == null
                ? target
                : transformer.nearestCopy(new BlockPos(centreX, target.getY(), centreZ), target);
    }

    public static Vec3 centreInMapFrame(ResourceKey<Level> dimension, int centreX, int centreZ, double x, double z) {
        Vec3 raw = new Vec3(x, 0.0, z);
        WorldFold transformer = transformerFor(dimension);
        return transformer == null ? raw : transformer.nearestCopy(new Vec3(centreX, 0.0, centreZ), raw);
    }

    private static @Nullable WorldFold transformerFor(ResourceKey<Level> dimension) {
        return MapSeamFold.transformerFor(null, dimension);
    }

    private CreateStationMapFold() {
    }
}
