package com.toroidalworld.compat.c2me.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.accessors.TransformerSourceBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;

// The manager is the whole context C2ME's worldgen write lock carries: runTaskWithLockArea is static and takes nothing
// else, so a fold that needs the level has to find it here. One manager exists per ChunkMap, which makes it a level in
// everything but name.
//
// It delegates rather than resolving: the chunk system built on the same ChunkMap already resolves the transformer and
// keeps it, and its resolve is deliberately lazy because the generator does not exist while the level is still being
// built. Binding the source instead of the answer keeps that laziness and leaves one resolve in the game.
@Mixin(SchedulingManager.class)
public class SchedulingManagerMixin implements TransformerSource, TransformerSourceBindable {
    // Written once on the thread that builds the chunk system, read on every worker that takes a lock.
    @Unique
    private volatile @Nullable TransformerSource toroidal$source;

    @Override
    public void toroidal$bindTransformerSource(TransformerSource source) {
        this.toroidal$source = source;
    }

    @Override
    public @Nullable WorldLoopTransformer toroidal$wrappedTransformer() {
        TransformerSource source = this.toroidal$source;
        return source != null ? source.toroidal$wrappedTransformer() : null;
    }
}
