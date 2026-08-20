package com.toroidalworld.entity;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SeamSteering {
    public static Vec3 nearestCopy(Entity body, Vec3 target) {
        WorldLoopTransformer transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return transformer == null ? target : transformer.vectors.nearestCopy(body.position(), target);
    }

    public static BlockPos nearestCopy(Entity body, BlockPos target) {
        WorldLoopTransformer transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return transformer == null ? target : transformer.blocks.nearestCopy(body.blockPosition(), target);
    }

    private SeamSteering() {
    }
}
