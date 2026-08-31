package com.toroidalworld.compat.sable;

import java.util.List;

import com.toroidalworld.core.FoldedBoxQuery;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class SableEntityQuery {
    public static List<AABB> pieces(Level level, AABB box) {
        return FoldedBoxQuery.pieces(WorldLoopAttachments.wrappedTransformerOfReader(level), box);
    }

    private SableEntityQuery() {
    }
}
