package com.toroidalworld.compat.c2me.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.accessors.TransformerSourceBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;

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
