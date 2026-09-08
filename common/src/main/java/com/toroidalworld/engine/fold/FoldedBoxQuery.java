package com.toroidalworld.engine.fold;

import com.toroidalworld.core.WorldFold;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FoldedBoxQuery {
    public static AABB toward(@Nullable WorldFold fold, Vec3 anchor, AABB box) {
        return fold == null ? box : fold.foldBox(anchor, box).value();
    }

    private FoldedBoxQuery() {
    }
}
