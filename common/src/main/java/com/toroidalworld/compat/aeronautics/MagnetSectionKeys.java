package com.toroidalworld.compat.aeronautics;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;

public final class MagnetSectionKeys {
    public static @Nullable SectionPos physical(LevelAccessor level, @Nullable SectionPos raw) {
        return fold(WorldLoopAttachments.transformerOfReader(level), raw);
    }

    public static @Nullable SectionPos physical(LevelAccessor level, LocalRef<WorldFold> memo,
            @Nullable SectionPos raw) {
        WorldFold fold = memo.get();
        if (fold == null) {
            fold = WorldLoopAttachments.transformerOfReader(level);
            memo.set(fold);
        }

        return fold(fold, raw);
    }

    private static @Nullable SectionPos fold(WorldFold fold, @Nullable SectionPos raw) {
        return raw == null ? null : fold.fold(raw);
    }

    private MagnetSectionKeys() {
    }
}
