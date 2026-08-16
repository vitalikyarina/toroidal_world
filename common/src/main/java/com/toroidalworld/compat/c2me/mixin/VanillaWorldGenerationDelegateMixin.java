package com.toroidalworld.compat.c2me.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.compat.c2me.C2meSeamFold;
import com.toroidalworld.core.WorldLoopTransformer;
import com.ishland.c2me.base.common.scheduler.LockTokenImpl;
import com.ishland.c2me.base.common.scheduler.ScheduledTask;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.VanillaWorldGenerationDelegate;
import com.ishland.flowsched.executor.LockToken;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;

// C2ME's replacement for ChunkGenerationTask: it builds the neighbourhood square itself and hands it straight to
// ChunkStep.apply, so ChunkGenerationTask.create is never called and the mod's own slot fold never runs. This is that
// fold restated where the square is actually built.
//
// Both squares are wrapped by the one handler — the loading branch, taken when the chunk on disk already carries the
// status, and the generation branch. Their radii differ (loadDepsRadius against genDepsRadius) but the statement does
// not.
//
// The initializer is substituted rather than the getHolder call inside it: that call lives in a lambda, and a handler
// scoped to this method would match nothing at all (see conventions.md, and the same reason the vanilla-shaped mixin
// wraps StaticCache2D.create).
@Mixin(VanillaWorldGenerationDelegate.class)
public class VanillaWorldGenerationDelegateMixin {
    @WrapOperation(
            method = "upgradeToThis",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/StaticCache2D;create(IIILnet/minecraft/util/StaticCache2D$Initializer;)Lnet/minecraft/util/StaticCache2D;"))
    private StaticCache2D<GenerationChunkHolder> toroidal$foldSeamSlots(
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
            return initializer.get(slot.x(), slot.z());
        };
        return original.call(centerX, centerZ, range, folding);
    }

    // The write lock is taken on the raw square, so a step whose square runs past the bounds locks chunks that do not
    // exist while writing, through the mod's own fold, into the ones across the seam — two tasks on opposite sides
    // hold disjoint tokens and the same physical chunk. Folding the token positions is what makes them meet.
    //
    // The whole method is wrapped rather than its call in upgradeToThis: that call sits inside a Completable.defer
    // lambda, and a handler bound to the enclosing method matches nothing (conventions.md), while one bound to
    // lambda$upgradeToThis$N holds only until C2ME is next recompiled. This is the primitive both helpers funnel
    // through, and the only place in C2ME that builds a worldgen lock token.
    //
    // The task's own position stays as C2ME computes it. It is the square's corner and it feeds the priority map, not
    // the lock — folding it would move a task between priority buckets to no purpose.
    @WrapMethod(method = "runTaskWithLockArea")
    private static <T> CompletableFuture<T> toroidal$foldLockArea(
            int baseChunkX,
            int baseChunkZ,
            int sizeX,
            int sizeZ,
            SchedulingManager schedulingManager,
            Supplier<CompletableFuture<T>> action,
            Operation<CompletableFuture<T>> original) {
        WorldLoopTransformer transformer = ((TransformerSource) schedulingManager).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(baseChunkX, baseChunkZ, sizeX, sizeZ, schedulingManager, action);
        }

        long[] positions = C2meSeamFold.canonicalLockPositions(transformer, baseChunkX, baseChunkZ, sizeX, sizeZ);
        LockToken[] tokens = new LockToken[positions.length];
        for (int tokenIdx = 0; tokenIdx < positions.length; tokenIdx++) {
            tokens[tokenIdx] =
                    new LockTokenImpl(schedulingManager.getId(), positions[tokenIdx], LockTokenImpl.Usage.WORLDGEN);
        }

        ScheduledTask<T> task = new ScheduledTask<>(ChunkPos.pack(baseChunkX, baseChunkZ), action, tokens);
        schedulingManager.enqueue(task);
        return task.getFuture();
    }
}
