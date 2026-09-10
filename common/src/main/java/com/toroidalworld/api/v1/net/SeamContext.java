package com.toroidalworld.api.v1.net;

import com.toroidalworld.api.v1.ToroidalShape;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * The two frames one player's connection has, handed to a rewriter for the packet it is rewriting. The server holds
 * every position folded into the world's bounds; the client holds whichever copy it was sent, which near the seam
 * runs whole world widths away. A position leaving the server has to be moved into the frame that client holds, and
 * one arriving has to be folded back.
 *
 * <p>{@code toClient} takes a canonical position to the copy this client holds — the one to write into anything the
 * client will draw, place or measure against. {@code toServer} folds a position the client sent back into the
 * world's bounds. Both hand the argument instance itself back when nothing moved.</p>
 *
 * <p>A context belongs to the packet being rewritten and to nothing else: never hold one past the call.</p>
 */
public interface SeamContext {

    /** The geometry of the level this connection is in — every fold not tied to the client's frame. */
    ToroidalShape shape();

    /** The copy of {@code pos} this client holds, seated in the copy of its own chunk. */
    BlockPos toClient(BlockPos pos);

    /** The copy of {@code pos} this client holds. */
    Vec3 toClient(Vec3 pos);

    /** The copy of {@code chunkPos} this client holds. */
    ChunkPos toClient(ChunkPos chunkPos);

    /** {@code pos} as the client sent it, folded back into the world's bounds. */
    BlockPos toServer(BlockPos pos);

    /** {@code pos} as the client sent it, folded back into the world's bounds. */
    Vec3 toServer(Vec3 pos);
}
