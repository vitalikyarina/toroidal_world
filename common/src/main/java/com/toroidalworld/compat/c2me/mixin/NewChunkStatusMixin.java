package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.compat.c2me.C2meSeamFold;
import com.toroidalworld.core.WorldLoopTransformer;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.ishland.flowsched.scheduler.ItemHolder;

import net.minecraft.world.level.ChunkPos;

// The one place every neighbourhood C2ME waits on is named. Each status declares its dependencies as offsets, and this
// is where the offsets become absolute keys: the generation squares taken from the vanilla pyramid, the eight
// neighbours ServerBlockTicking needs at FULL, and the eight ServerEntityTicking needs at BLOCK_TICKING. Folding here
// restates in one place what two vanilla-shaped mixins say separately — the loading graph of ChunkGenerationTaskMixin
// and the promotion square of ChunkMapMixin.getChunkRangeFuture, the one whose raw keys once left the world hanging on
// spawn and the band along the seam never sent.
//
// No centre to spare: every relative offset that reaches here is a neighbour, (0,0) being skipped where the sets are
// built. So a dependency key is folded unconditionally, and no holder is ever asked for past the bounds.
@Mixin(NewChunkStatus.class)
public class NewChunkStatusMixin {
    @WrapOperation(
            method = "relativeToAbsoluteDependencies",
            at = @At(value = "NEW", target = "(II)Lnet/minecraft/world/level/ChunkPos;"))
    private static ChunkPos toroidal$canonicalDependencyKey(
            int chunkX,
            int chunkZ,
            Operation<ChunkPos> original,
            @Local(argsOnly = true) ItemHolder<?, ?, ?, ?> holder) {
        WorldLoopTransformer transformer = holder.getUserData().get() instanceof TransformerSource source
                ? source.toroidal$wrappedTransformer()
                : null;
        if (transformer == null) {
            return original.call(chunkX, chunkZ);
        }

        return C2meSeamFold.canonical(transformer, chunkX, chunkZ);
    }
}
