package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.compat.c2me.C2meSeamFold;
import com.toroidalworld.core.WorldLoopTransformer;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessibleChunkSending;
import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;

// The fifth square, and the one that is not in C2ME's chunk-system module at all: no-tick view distance overwrites
// this status's upgradeToThis and builds a WorldGenRegion around the chunk and its eight neighbours, to drop the
// mushrooms a non-postprocessed chunk would otherwise show. Without notickvd the method is an empty Completable and
// this injection has nothing to attach to, which is why it is gated on that module rather than on the chunk system.
//
// It reads as the same statement as the other squares because it is: a neighbour at the bounds is the chunk across
// the seam. Unfolded, it asked for a holder that was never raised and took the NullPointerException that stopped the
// chunk from ever reaching SERVER_ACCESSIBLE — which is what left the seam band unsent and the server thread blocked
// on a chunk that could not arrive.
//
// Reached through MixinSquared and not by method name: this class's own upgradeToThis is an empty Completable, and
// the square exists only in the body notickvd overwrites it with. A plain @WrapOperation searches the class as it
// stands and finds nothing to attach to — "Scanned 0 target(s)" — whereas @TargetHandler resolves the method by the
// mixin that contributes it.
@Mixin(ServerAccessibleChunkSending.class)
public class ServerAccessibleChunkSendingMixin {
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/StaticCache2D;create(IIILnet/minecraft/util/StaticCache2D$Initializer;)Lnet/minecraft/util/StaticCache2D;"))
    @TargetHandler(
            mixin = "com.ishland.c2me.notickvd.mixin.MixinServerAccessibleChunkSending",
            name = "upgradeToThis")
    private StaticCache2D<GenerationChunkHolder> toroidal$foldRegionSlots(
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
