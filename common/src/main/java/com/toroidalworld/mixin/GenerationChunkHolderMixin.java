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
