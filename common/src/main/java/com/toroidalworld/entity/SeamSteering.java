package com.toroidalworld.entity;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SeamSteering {
    public static Vec3 nearestCopy(Entity body, Vec3 target) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return transformer == null ? target : transformer.nearestCopy(body.position(), target);
    }

    public static BlockPos nearestCopy(Entity body, BlockPos target) {
        WorldFold transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return transformer == null ? target : transformer.nearestCopy(body.blockPosition(), target);
    }

    private SeamSteering() {
    }
}
