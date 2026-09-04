package com.toroidalworld.compat.aeronautics;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;

public final class MagnetSectionKeys {
    public static @Nullable SectionPos physical(LevelAccessor level, @Nullable SectionPos raw) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(level);
        return fold == null || raw == null ? raw : fold.fold(raw);
    }

    private MagnetSectionKeys() {
    }
}
