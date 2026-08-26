package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Unique
    private WorldFold toroidal$transformer;

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapRequestedChunkX(int chunkX) {
        return toroidal$transformer().chunkDomain(Direction.Axis.X).wrap(chunkX);
    }

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapRequestedChunkZ(int chunkZ) {
        return toroidal$transformer().chunkDomain(Direction.Axis.Z).wrap(chunkZ);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapChunkX(int chunkX) {
        return toroidal$transformer().chunkDomain(Direction.Axis.X).wrap(chunkX);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapChunkZ(int chunkZ) {
        return toroidal$transformer().chunkDomain(Direction.Axis.Z).wrap(chunkZ);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapPresenceChunkX(int chunkX) {
        return toroidal$transformer().chunkDomain(Direction.Axis.X).wrap(chunkX);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapPresenceChunkZ(int chunkZ) {
        return toroidal$transformer().chunkDomain(Direction.Axis.Z).wrap(chunkZ);
    }

    @ModifyVariable(method = "blockChanged", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapChangedBlock(BlockPos pos) {
        return toroidal$transformer().fold(pos);
    }

    @Unique
    private WorldFold toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = WorldLoopAttachments.transformerOf(this.level);
        }

        return this.toroidal$transformer;
    }
}
