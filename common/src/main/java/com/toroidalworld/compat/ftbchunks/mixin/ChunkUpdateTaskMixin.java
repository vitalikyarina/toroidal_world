package com.toroidalworld.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import dev.ftb.mods.ftbchunks.client.map.ChunkUpdateTask;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftblibrary.math.XZ;

// Only the keys fold: the client level holds its chunks where the server sent them, so the rest of the task reads
// the world at the raw position.
@Mixin(value = ChunkUpdateTask.class, remap = false)
public abstract class ChunkUpdateTaskMixin {
    @Shadow
    private MapManager manager;

    @Shadow
    @Final
    private Level level;

    @Unique
    private int toroidal$rawChunkX;

    @Unique
    private int toroidal$rawChunkZ;

    @Unique
    private XZ toroidal$regionKey;

    @WrapOperation(method = "runMapTask",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/math/XZ;regionFromChunk(Lnet/minecraft/world/level/ChunkPos;)"
                            + "Ldev/ftb/mods/ftblibrary/math/XZ;"))
    private XZ toroidal$foldRegionKey(ChunkPos pos, Operation<XZ> original) {
        toroidal$rawChunkX = pos.x;
        toroidal$rawChunkZ = pos.z;
        toroidal$regionKey = FtbChunksFold.regionOfChunk(pos);
        return toroidal$regionKey;
    }

    @WrapOperation(method = "runMapTask",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/math/XZ;of(Lnet/minecraft/world/level/ChunkPos;)"
                            + "Ldev/ftb/mods/ftblibrary/math/XZ;"))
    private XZ toroidal$foldChunkKey(ChunkPos pos, Operation<XZ> original) {
        return FtbChunksFold.chunkOf(pos);
    }

    @Inject(method = "runMapTask", at = @At("TAIL"))
    private void toroidal$mirrorSeamEdges(CallbackInfo ci) {
        if (this.manager == null || this.manager.isInvalid() || toroidal$regionKey == null) {
            return;
        }

        XZ folded = FtbChunksFold.chunkOf(toroidal$rawChunkX, toroidal$rawChunkZ);
        FtbChunksFold.mirrorSeamEdges(this.manager.getDimension(this.level.dimension()), folded.x(), folded.z());
    }
}
