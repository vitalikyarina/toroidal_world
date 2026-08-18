package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// What Create needs from us, in two statements over one fold. The kinetic family asks for a delta between two blocks
// measured the short way round the world: Create turns that delta into a Direction, so a delta reading a world wide
// names the opposite side, and the neighbour Level.getBlockEntity has already handed back correctly is then described
// backwards. The belt connector and the fluid behaviours ask for the folded position itself, because what they do with
// it is not one subtraction but a whole run of them — a length gate, a shape, a walk of the gap, a flood fill — and
// one coordinate put into the right frame answers every one of them.
//
// There is no primitive to wrap here. BlockPos.subtract and BlockPos.distSqr carry no level, and each Create site
// spells its arithmetic out again for itself, so the statement lives in one place and every site's mixin calls it with
// its own two positions. It is taken at the read and never at the store: source positions are compared with equals
// against canonical positions elsewhere in Create, and a folded field would break that identity for nothing.
//
// The two entry points of each pair differ only in which bounds answer. On the server the level's own transformer is
// the truth. On the client that transformer is deliberately NOOP — the client is told the world is infinite — so a
// value read there folds by the bounds the server sent instead. A client site needs that second reading because the
// position it starts from crossed as an absolute server coordinate, in a block entity tag or an item component, while
// the position it is measured against is the client's own mirror; the two are the same space only until the player
// laps the world or the pair straddles the seam.
public final class CreateSeamFold {
    // anchor is the block the delta is measured from, target the one it points at. rawDelta is what the subtraction
    // itself produced, handed in rather than recomputed so that a pair with nothing to fold gets that very object back
    // — which is what Create would have held, down to its identity.
    public static BlockPos foldDelta(@Nullable Level level, BlockPos anchor, BlockPos target, BlockPos rawDelta) {
        if (level == null) {
            return rawDelta;
        }

        return delta(WorldLoopAttachments.wrappedTransformerOf(level), anchor, target, rawDelta);
    }

    // The client's counterpart, and the reason it cannot share the lookup above: the client level's own transformer
    // says the world is infinite, so it would fold nothing and leave the delta reading a world wide.
    public static BlockPos foldClientDelta(@Nullable Level level, BlockPos anchor, BlockPos target, BlockPos rawDelta) {
        if (level == null) {
            return rawDelta;
        }

        return delta(WorldLoopAttachments.wrappedClientBoundsTransformerOf(level), anchor, target, rawDelta);
    }

    // The copy of target lying nearest anchor — which may sit past the bounds, and is meant to. A caller that folds a
    // position rather than a delta goes on to measure, walk and write from it, and every one of those reaches the world
    // through Level, which wraps the coordinate it is handed. Keeping the frame instead of the bounds is what lets the
    // arithmetic in between stay ordinary.
    public static BlockPos foldPosition(@Nullable Level level, BlockPos anchor, BlockPos target) {
        if (level == null) {
            return target;
        }

        return nearest(WorldLoopAttachments.wrappedTransformerOf(level), anchor, target);
    }

    public static BlockPos foldClientPosition(@Nullable Level level, BlockPos anchor, BlockPos target) {
        if (level == null) {
            return target;
        }

        return nearest(WorldLoopAttachments.wrappedClientBoundsTransformerOf(level), anchor, target);
    }

    private static BlockPos delta(@Nullable WorldLoopTransformer transformer, BlockPos anchor, BlockPos target,
            BlockPos rawDelta) {
        BlockPos nearest = nearest(transformer, anchor, target);
        if (nearest.equals(target)) {
            return rawDelta;
        }

        return nearest.subtract(anchor);
    }

    private static BlockPos nearest(@Nullable WorldLoopTransformer transformer, BlockPos anchor, BlockPos target) {
        if (transformer == null) {
            return target;
        }

        return transformer.blocks.nearestCopy(anchor, target);
    }

    private CreateSeamFold() {
    }
}
