package com.toroidalworld.engine.seam;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.fold.NearestCopy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SeamSteering {
    public static Vec3 nearestCopy(Entity body, Vec3 target) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return NearestCopy.toward(transformer, body.position(), target);
    }

    public static BlockPos nearestCopy(Entity body, BlockPos target) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return NearestCopy.toward(transformer, body.blockPosition(), target);
    }

    private SeamSteering() {
    }
}
