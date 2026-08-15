package com.toroidalworld.compat.c2me.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.TheChunkSystem;

// The dependency sets are computed from a flowsched ItemHolder, which knows its key and its user data and nothing
// about any level. This is the step from that user data back to the level the holder belongs to.
@Mixin(NewChunkHolderVanillaInterface.class)
public class NewChunkHolderVanillaInterfaceMixin implements TransformerSource {
    @Shadow
    @Final
    private TheChunkSystem chunkSystem;

    @Override
    public @Nullable WorldLoopTransformer toroidal$wrappedTransformer() {
        return ((TransformerSource) this.chunkSystem).toroidal$wrappedTransformer();
    }
}
