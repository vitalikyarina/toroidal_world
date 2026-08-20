package com.toroidalworld.entity;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SeamAim {
    public static double nearX(Entity viewer, double targetX) {
        WorldLoopTransformer transformer = ((TransformerSource) viewer).toroidal$wrappedTransformer();
        return transformer == null ? targetX : transformer.coords.x.unwrapAround(viewer.getX(), targetX);
    }

    public static double nearZ(Entity viewer, double targetZ) {
        WorldLoopTransformer transformer = ((TransformerSource) viewer).toroidal$wrappedTransformer();
        return transformer == null ? targetZ : transformer.coords.z.unwrapAround(viewer.getZ(), targetZ);
    }

    public static Vec3 nearestTo(Entity viewer, Vec3 point) {
        return SeamSteering.nearestCopy(viewer, point);
    }

    public static double foldX(Entity levelSource, double delta) {
        WorldLoopTransformer transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        return transformer == null ? delta : transformer.coords.x.foldDelta(delta);
    }

    public static double foldZ(Entity levelSource, double delta) {
        WorldLoopTransformer transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        return transformer == null ? delta : transformer.coords.z.foldDelta(delta);
    }

    public static Vec3 foldDelta(Entity levelSource, Vec3 delta) {
        WorldLoopTransformer transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        if (transformer == null) {
            return delta;
        }

        double foldedX = transformer.coords.x.foldDelta(delta.x);
        double foldedZ = transformer.coords.z.foldDelta(delta.z);
        return foldedX == delta.x && foldedZ == delta.z ? delta : new Vec3(foldedX, delta.y, foldedZ);
    }

    private SeamAim() {
    }
}
