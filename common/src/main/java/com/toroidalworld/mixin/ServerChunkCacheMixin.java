package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Unique
    private WorldLoopTransformer toroidal$transformer;

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapRequestedChunkX(int chunkX) {
        return toroidal$transformer().chunks.x.wrap(chunkX);
    }

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapRequestedChunkZ(int chunkZ) {
        return toroidal$transformer().chunks.z.wrap(chunkZ);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapChunkX(int chunkX) {
        return toroidal$transformer().chunks.x.wrap(chunkX);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapChunkZ(int chunkZ) {
        return toroidal$transformer().chunks.z.wrap(chunkZ);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapPresenceChunkX(int chunkX) {
        return toroidal$transformer().chunks.x.wrap(chunkX);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapPresenceChunkZ(int chunkZ) {
        return toroidal$transformer().chunks.z.wrap(chunkZ);
    }

    // Writing a block past the bounds already lands in the real chunk on the other side, but the notification looks up
    // its chunk holder by the raw position — an out-of-bounds one, which no player tracks. The block would change on the
    // server and never be heard of again: the far half of a bed, the crater of an explosion across the seam.
    @ModifyVariable(method = "blockChanged", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapChangedBlock(BlockPos pos) {
        return toroidal$transformer().blocks.wrap(pos);
    }

    // getChunkNow is one of the hottest paths in the server, and the cache's level never changes. Deliberately not
    // volatile: resolution is idempotent — transformerOf hands back the level's one attachment instance — so a race can
    // only cost a repeated lookup, never a second transformer.
    @Unique
    private WorldLoopTransformer toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = WorldLoopAttachments.transformerOf(this.level);
        }

        return this.toroidal$transformer;
    }
}
