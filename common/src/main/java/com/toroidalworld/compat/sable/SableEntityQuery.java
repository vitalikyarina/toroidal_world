package com.toroidalworld.compat.sable;

import java.util.List;

import com.toroidalworld.core.FoldedBoxQuery;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

// The engine fold, not the client-bounds accessor the nearest-copy reads in this package take: a bounds split against client bounds relocates a client query into the canonical lap, where the client holds nothing.
public final class SableEntityQuery {
    public static List<AABB> pieces(Level level, AABB box) {
        return FoldedBoxQuery.pieces(WorldLoopAttachments.transformerOf(level), box);
    }

    private SableEntityQuery() {
    }
}
