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

// Code all over the server asks for a chunk by coordinates that may sit past the world bounds — a mob pathing across
// the seam, a block update at the edge. Those coordinates name a real chunk on the other side, so they are wrapped.
@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Unique
    private WorldLoopTransformer toroidal$transformer;

    // The synchronous chunk request stores an UNKNOWN ticket under the key it was asked with and immediately re-reads
    // the holder by that same key — "No chunk holder after ticket has been added" if the two ever disagree. With the
    // ticket key folded in the graphs (DistanceManagerMixin) a raw out-of-bounds request would trip exactly that, so
    // the request itself is folded at the entry: the ticket, the graph run and the holder lookup then all name the one
    // chunk that exists.
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

    // "Is there a chunk here" is asked of a raw position all over game logic — lava deciding whether to spread, a raid
    // checking its centre, /clone measuring the box it is about to paste into — and past the bounds it answers no about
    // ground that plainly exists on the other side, so the caller quietly does nothing. Both primitives that ask it,
    // LevelAccessor.hasChunk and Level.isLoaded, end here, so folding it onto the physical chunk once covers every
    // server-side asker. The client answers its own ClientLevel.hasChunk and worldgen its own WorldGenRegion.hasChunk;
    // neither reaches this.
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
