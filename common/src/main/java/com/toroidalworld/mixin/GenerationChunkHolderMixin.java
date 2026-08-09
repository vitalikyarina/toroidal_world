package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.SeamDriveScheduler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GeneratingChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

// A generation task may only generate the chunk it is centred on and the neighbours in its own square. Once a slot is
// folded across the seam, the holder sitting in it belongs to a different frame: its step would build a WorldGenRegion
// centred on the wrapped chunk and ask for THAT chunk's neighbours, which this task's cache does not contain and cannot
// — hence "Requested out of range value (3,-9) from StaticCache2D[-5, -3, 16, 18]".
//
// Vanilla already has the branch we want. applyStep runs the step only when it wins the startedWork CAS; whoever loses
// waits on the future the winner will complete. Declining the CAS for a holder outside this cache turns the foreign
// task into a waiter — it reads the neighbour's result and never generates it. The predicate is exact on a straddling
// world too: a physical chunk whose canonical position falls inside the raw square IS steppable in this task's frame,
// however many folded slots also carry it — the duplicates just lose the CAS and wait.
//
// A decline leaves nobody generating the holder: vanilla gives tasks only to chunks somebody asks for, and a foreign
// task's ask arrives here, not at the scheduler. The decline therefore files a drive request, and the chunk map answers
// it with a task of the holder's own (SeamDriveScheduler, drained in runGenerationTasks on the server thread). If the
// holder's level has dropped by then, scheduleChunkGenerationTask refuses and the same drop has already failed the
// future the foreign task waits on — vanilla's cancel path, correct and loud.
@Mixin(GenerationChunkHolder.class)
public abstract class GenerationChunkHolderMixin {
    @Shadow
    public abstract ChunkPos getPos();

    @WrapOperation(
            method = "applyStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/GenerationChunkHolder;acquireStatusBump(Lnet/minecraft/world/level/chunk/status/ChunkStatus;)Z"))
    private boolean toroidal$neverStepAcrossFrames(
            GenerationChunkHolder holder,
            ChunkStatus status,
            Operation<Boolean> original,
            @Local(argsOnly = true) StaticCache2D<GenerationChunkHolder> cache,
            @Local(argsOnly = true) GeneratingChunkMap chunkMap) {
        ChunkPos pos = this.getPos();
        if (cache.contains(pos.x, pos.z)) {
            return original.call(holder, status);
        }

        if (chunkMap instanceof ChunkMap map) {
            ((SeamDriveScheduler) (Object) map).toroidal$requestDrive(holder, status);
        }

        return false;
    }
}
