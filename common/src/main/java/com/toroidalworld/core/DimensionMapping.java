package com.toroidalworld.core;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class DimensionMapping {
    public static Vec3 map(WorldFold source, WorldFold destination, Vec3 position, double declaredScale) {
        double mappedX = destination.blockDomain(Direction.Axis.X)
                .mapFrom(source.blockDomain(Direction.Axis.X), position.x, declaredScale);
        double mappedZ = destination.blockDomain(Direction.Axis.Z)
                .mapFrom(source.blockDomain(Direction.Axis.Z), position.z, declaredScale);
        return mappedX == position.x && mappedZ == position.z ? position : new Vec3(mappedX, position.y, mappedZ);
    }

    private DimensionMapping() {
    }
}
