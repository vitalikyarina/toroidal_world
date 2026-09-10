package com.toroidalworld.api.v1.client;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.client.engine.ClientFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * The two anchors a client already has — the player and the camera — and the copy of a position the client is
 * actually holding.
 *
 * <p>{@link com.toroidalworld.api.v1.ToroidalShape#nearestCopy(Vec3, Vec3)} needs a reference to seat a position
 * against. On the client that reference is nearly always one of these two, and reaching for the wrong one shows:
 * something drawn folds toward the camera, not the player, and in third person or a spectator's view those are
 * different places. These take the anchor off the running client, so a caller states which one it means and
 * nothing else.</p>
 *
 * <p>Client-side only, and safe before a world is loaded: with no level, no player or no folding world, every
 * member hands the argument straight back, so a call needs no guard of its own.</p>
 */
public final class ClientAnchors {

    /** The copy of {@code target} nearest the player. */
    public static @Nullable BlockPos nearestToPlayer(@Nullable BlockPos target) {
        return ClientFrame.nearestToPlayer(target);
    }

    /** The copy of {@code target} nearest the player. */
    public static @Nullable Vec3 nearestToPlayer(@Nullable Vec3 target) {
        return ClientFrame.nearestToPlayer(target);
    }

    /** The copy of {@code target} nearest the player. */
    public static @Nullable ChunkPos nearestToPlayer(@Nullable ChunkPos target) {
        return ClientFrame.nearestToPlayer(target);
    }

    /** The copy of {@code target} nearest the camera — what anything drawn in the world is seated against. */
    public static @Nullable Vec3 nearestToCamera(@Nullable Vec3 target) {
        return ClientFrame.nearestToCamera(target);
    }

    /**
     * The copy of {@code canonical} whose chunk the client is holding right now, or {@code null} when it holds
     * none of them.
     *
     * <p>A client near a seam is sent one copy of a chunk and not the others, and which one it holds is the
     * server's choice, not the nearest. Anything keyed by a block position on the client — a block entity, a
     * rendered overlay, a cached state — has to be keyed by the copy that is actually there, and {@code null} is
     * the honest answer that there is nothing to key: treat it as "not loaded", never as the canonical position.</p>
     */
    public static @Nullable BlockPos heldCopy(BlockPos canonical) {
        return ClientFrame.heldCopy(canonical);
    }

    /**
     * {@link #heldCopy(BlockPos)} for a chunk, asking {@code holds} whether a given copy is the one present —
     * whatever "present" means to the caller's own storage.
     */
    public static @Nullable ChunkPos heldCopy(ChunkPos canonical, Predicate<ChunkPos> holds) {
        return ClientFrame.heldCopy(canonical, holds);
    }

    private ClientAnchors() {
    }
}
