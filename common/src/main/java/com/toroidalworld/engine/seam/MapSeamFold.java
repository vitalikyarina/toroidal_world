package com.toroidalworld.engine.seam;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.level.CurrentServer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public final class MapSeamFold {
    public static @Nullable WorldFold transformerFor(@Nullable LevelAccessor level, ResourceKey<Level> dimension) {
        if (level instanceof Level actualLevel) {
            return WorldLoopAttachments.wrappedTransformerOf(actualLevel);
        }

        return WorldLoopAttachments.wrappedTransformerOf(CurrentServer.get(), dimension);
    }

    private MapSeamFold() {
    }
}
