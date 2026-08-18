package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// The one statement Create's kinetic family needs from us: the delta between two blocks, measured the short way round
// the world. Create turns that delta into a Direction, so a delta reading a world wide names the opposite side — and
// the neighbour Level.getBlockEntity has already handed back correctly is then described backwards.
//
// There is no primitive to wrap here. BlockPos.subtract carries no level, and each Create site spells the subtraction
// out again for itself, so the statement lives in one place and every site's mixin calls it with its own two
// positions. It is taken at the read and never at the store: a source position is compared with equals against
// canonical positions elsewhere in the propagator, and a folded field would break that identity for nothing.
public final class CreateSeamFold {
    // anchor is the block the delta is measured from, target the one it points at. rawDelta is what the subtraction
    // itself produced, handed in rather than recomputed so that a pair with nothing to fold gets that very object back
    // — which is what Create would have held, down to its identity.
    public static BlockPos foldDelta(@Nullable Level level, BlockPos anchor, BlockPos target, BlockPos rawDelta) {
        if (level == null) {
            return rawDelta;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return rawDelta;
        }

        BlockPos nearest = transformer.blocks.nearestCopy(anchor, target);
        if (nearest.equals(target)) {
            return rawDelta;
        }

        return nearest.subtract(anchor);
    }

    private CreateSeamFold() {
    }
}
