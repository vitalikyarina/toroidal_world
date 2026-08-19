package com.toroidalworld.predicate;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SeamDistanceBounds {
    public static Vec3 nearestCopy(Level level, Vec3 reference, Vec3 measured) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? measured : transformer.vectors.nearestCopy(reference, measured);
    }

    private SeamDistanceBounds() {
    }
}
