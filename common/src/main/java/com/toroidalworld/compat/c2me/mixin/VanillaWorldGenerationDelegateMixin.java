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
            return initializer.get(slot.x, slot.z);
        };
        return original.call(centerX, centerZ, range, folding);
    }

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

        ScheduledTask<T> task = new ScheduledTask<>(ChunkPos.asLong(baseChunkX, baseChunkZ), action, tokens);
        schedulingManager.enqueue(task);
        return task.getFuture();
    }
}
