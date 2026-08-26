package com.toroidalworld.entity;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.SeamDelta;
import com.toroidalworld.core.WorldFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SeamAim {
    public static Vec3 nearestTo(Entity viewer, Vec3 point) {
        return SeamSteering.nearestCopy(viewer, point);
    }

    public static Vec3 deltaTo(Entity viewer, Vec3 point) {
        WorldFold transformer = ((TransformerSource) viewer).toroidal$wrappedTransformer();
        return transformer == null
                ? point.subtract(viewer.position())
                : transformer.foldDelta(viewer.position(), point);
    }

    public static double foldX(Entity levelSource, double delta) {
        WorldFold transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        return transformer == null ? delta : SeamDelta.foldX(transformer, delta);
    }

    public static double foldZ(Entity levelSource, double delta) {
        WorldFold transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        return transformer == null ? delta : SeamDelta.foldZ(transformer, delta);
    }

    public static Vec3 foldDelta(Entity levelSource, Vec3 delta) {
        WorldFold transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        if (transformer == null) {
            return delta;
        }

        return SeamDelta.fold(transformer, delta);
    }

    private SeamAim() {
    }
}
