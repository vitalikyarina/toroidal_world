package com.toroidalworld.compat.create;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class CreateContraptionFold {
    public static Vec3 inFrameOf(Entity entity, Vec3 canonical) {
        return CreateSeamFold.nearestCopy(entity.level(), entity.position(), canonical);
    }

    public static double axisInFrameOf(Entity entity, Direction.Axis axis, double canonicalCoord) {
        Vec3 position = entity.position();
        Vec3 folded = CreateSeamFold.nearestCopy(entity.level(), position, onAxis(position, axis, canonicalCoord));
        return axis.choose(folded.x, folded.y, folded.z);
    }

    private static Vec3 onAxis(Vec3 position, Direction.Axis axis, double coord) {
        return switch (axis) {
            case X -> new Vec3(coord, position.y, position.z);
            case Y -> new Vec3(position.x, coord, position.z);
            case Z -> new Vec3(position.x, position.y, coord);
        };
    }

    private CreateContraptionFold() {
    }
}
