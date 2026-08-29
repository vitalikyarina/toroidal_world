package com.toroidalworld.compat.aeronautics;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;

public final class MagnetSectionKeys {
    public static @Nullable SectionPos physical(LevelAccessor level, @Nullable SectionPos raw) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(level);
        return fold == null || raw == null ? raw : fold.fold(raw);
    }

    public static Object lookup(Map<?, ?> sections, Object key, LevelAccessor level, Operation<Object> original) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(level);
        if (fold == null || !(key instanceof SectionPos raw)) {
            return original.call(sections, key);
        }

        return original.call(sections, fold.fold(raw));
    }

    private MagnetSectionKeys() {
    }
}
