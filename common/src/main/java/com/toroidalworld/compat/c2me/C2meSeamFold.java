package com.toroidalworld.compat.c2me;

import com.toroidalworld.core.WorldLoopTransformer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.ChunkPos;

public final class C2meSeamFold {
    public static ChunkPos canonicalSlot(
            WorldLoopTransformer transformer, int centerX, int centerZ, int slotX, int slotZ) {
        if (slotX == centerX && slotZ == centerZ) {
            return new ChunkPos(slotX, slotZ);
        }

        return canonical(transformer, slotX, slotZ);
    }

    public static ChunkPos canonical(WorldLoopTransformer transformer, int chunkX, int chunkZ) {
        return new ChunkPos(transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
    }

    public static long[] canonicalLockPositions(
            WorldLoopTransformer transformer, int baseChunkX, int baseChunkZ, int sizeX, int sizeZ) {
        LongOpenHashSet folded = new LongOpenHashSet(sizeX * sizeZ);

        for (int offsetX = 0; offsetX < sizeX; offsetX++) {
            for (int offsetZ = 0; offsetZ < sizeZ; offsetZ++) {
                folded.add(ChunkPos.asLong(
                        transformer.chunks.x.wrap(baseChunkX + offsetX),
                        transformer.chunks.z.wrap(baseChunkZ + offsetZ)));
            }
        }

        return folded.toLongArray();
    }

    private C2meSeamFold() {
    }
}
