package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.compat.c2me.C2meSeamFold;
import com.toroidalworld.core.WorldLoopTransformer;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerBlockTicking;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;

// The third square C2ME builds: fluid post-processing wraps a WorldGenRegion around the chunk and its eight
// neighbours to decide which scheduled fluid ticks can be dropped. Same statement as the generation caches — a
// neighbour at the bounds is the chunk across the seam — and unfolded it would ask for a holder that does not exist
// and take an NPE on a worker thread, or judge the seam's fluids against the wrong side.
@Mixin(ServerBlockTicking.class)
public class ServerBlockTickingMixin {
    @WrapOperation(
            method = "filterFluidTicks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/StaticCache2D;create(IIILnet/minecraft/util/StaticCache2D$Initializer;)Lnet/minecraft/util/StaticCache2D;"))
    private static StaticCache2D<GenerationChunkHolder> toroidal$foldRegionSlots(
            int centerX,
            int centerZ,
            int range,
            StaticCache2D.Initializer<GenerationChunkHolder> initializer,
            Operation<StaticCache2D<GenerationChunkHolder>> original,
            @Local(argsOnly = true) ChunkLoadingContext context) {
        WorldLoopTransformer transformer =
                ((TransformerSource) context.theChunkSystem()).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(centerX, centerZ, range, initializer);
        }

        StaticCache2D.Initializer<GenerationChunkHolder> folding = (slotX, slotZ) -> {
            ChunkPos slot = C2meSeamFold.canonicalSlot(transformer, centerX, centerZ, slotX, slotZ);
            return initializer.get(slot.x, slot.z);
        };
        return original.call(centerX, centerZ, range, folding);
    }
}
