package com.toroidalworld.compat.c2me.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.TheChunkSystem;

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
