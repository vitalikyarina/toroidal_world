package com.toroidalworld.compat.c2me;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;

public final class C2meSeamFold {
    public static <T> StaticCache2D.Initializer<T> foldingInitializer(
            ChunkLoadingContext context,
            int centerX,
            int centerZ,
            StaticCache2D.Initializer<T> initializer) {
        WorldFold transformer = ((TransformerSource) context.theChunkSystem()).toroidal$wrappedTransformer();
        return foldingInitializer(transformer, centerX, centerZ, initializer);
    }

    private static <T> StaticCache2D.Initializer<T> foldingInitializer(
            @Nullable WorldFold transformer,
            int centerX,
            int centerZ,
            StaticCache2D.Initializer<T> initializer) {
        if (transformer == null) {
            return initializer;
        }

        return (slotX, slotZ) -> {
            ChunkPos slot = canonicalSlot(transformer, centerX, centerZ, slotX, slotZ);
            return initializer.get(slot.x(), slot.z());
        };
    }

    private static ChunkPos canonicalSlot(
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
                folded.add(transformer.foldChunkKey(ChunkPos.pack(baseChunkX + offsetX, baseChunkZ + offsetZ)));
            }
        }

        return folded.toLongArray();
    }

    private C2meSeamFold() {
    }
}
