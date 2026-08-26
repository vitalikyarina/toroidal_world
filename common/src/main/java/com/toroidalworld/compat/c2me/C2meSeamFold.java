package com.toroidalworld.compat.c2me;

import com.toroidalworld.core.WorldFold;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.ChunkPos;

public final class C2meSeamFold {
    public static ChunkPos canonicalSlot(
            WorldFold transformer, int centerX, int centerZ, int slotX, int slotZ) {
        if (slotX == centerX && slotZ == centerZ) {
            return new ChunkPos(slotX, slotZ);
        }

        return canonical(transformer, slotX, slotZ);
    }

    public static ChunkPos canonical(WorldFold transformer, int chunkX, int chunkZ) {
        return transformer.fold(new ChunkPos(chunkX, chunkZ));
    }

    public static long[] canonicalLockPositions(
            WorldFold transformer, int baseChunkX, int baseChunkZ, int sizeX, int sizeZ) {
        LongOpenHashSet folded = new LongOpenHashSet(sizeX * sizeZ);

        for (int offsetX = 0; offsetX < sizeX; offsetX++) {
            for (int offsetZ = 0; offsetZ < sizeZ; offsetZ++) {
                folded.add(transformer.foldChunkKey(ChunkPos.asLong(baseChunkX + offsetX, baseChunkZ + offsetZ)));
            }
        }

        return folded.toLongArray();
    }

    private C2meSeamFold() {
    }
}
