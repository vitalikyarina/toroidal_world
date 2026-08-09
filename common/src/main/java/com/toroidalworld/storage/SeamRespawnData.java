package com.toroidalworld.storage;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.probe.ReshapeProbe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

// A respawn point is one of the few coordinates the server keeps rather than spends. Everything else a command names is
// consumed on the spot — a block written, an entity moved, a packet sent — and each of those paths already folds the
// seam on its way to the world, so a coordinate arriving from past the bounds lands where it means to. This one is
// written down instead: into the player's data or into level.dat, where it outlives the command, the session and the
// restart, and where nothing folds anything.
//
// So this is where the wrap belongs — at the moment of storage, not on the way in. The level is the caller's to name,
// and it is always the one whose bounds the coordinate will be read back against: the player's respawn dimension, not
// the level the command was run in, because /spawnpoint stores a point that a respawn may consult from anywhere.
//
// A coordinate already inside the world is handed back as the very same instance, so a spawn point set anywhere in
// bounds keeps the exact position vanilla built — which is also what vanilla's own "did anything change" comparisons
// are made against.
public final class SeamRespawnData {
    // The fold name is here for the probe rather than for the arithmetic: every one of the five ways a spawn is stored
    // arrives at this one method, so the counters can only tell them apart if the caller says which it is.
    public static BlockPos insideBounds(ServerLevel level, String fold, BlockPos pos) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            ReshapeProbe.unwrapped(level.dimension(), fold);
            return pos;
        }

        BlockPos stored = transformer.blocks.wrap(pos);
        ReshapeProbe.fold(level.dimension(), fold, pos, stored);
        return stored;
    }

    private SeamRespawnData() {
    }
}
