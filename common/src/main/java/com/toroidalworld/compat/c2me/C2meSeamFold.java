package com.toroidalworld.compat.c2me;

import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.world.level.ChunkPos;

// One statement, worn by every neighbourhood square C2ME builds: the neighbour of a chunk at the bounds IS the chunk
// across the seam. C2ME spells such a square out in three places — the dependency set of every status, the two
// generation caches in VanillaWorldGenerationDelegate, and the region cache in ServerBlockTicking — and this is the
// single implementation all of them attach to. It is the same statement com.toroidalworld.mixin.ChunkGenerationTaskMixin
// makes for the vanilla shape, and it is deliberately NOT made inside the scheduler's own getHolder: there a key means
// "this exact holder", and folding it would hand back a holder whose getKey() is not the key asked for — an identity
// C2ME's own code reads back (relativeToAbsoluteDependencies builds from holder.getKey()).
//
// The square's own centre is never folded, whichever side of the bounds it is on: the task exists to advance THAT
// chunk, and a folded centre would advance a different holder's status.
public final class C2meSeamFold {
    // The slot as it physically is. Coordinates only: what each caller then does with the key — look a holder up,
    // declare a dependency, take a lock — is its own business, and none of them can be answered here.
    public static ChunkPos canonicalSlot(
            WorldLoopTransformer transformer, int centerX, int centerZ, int slotX, int slotZ) {
        if (slotX == centerX && slotZ == centerZ) {
            return new ChunkPos(slotX, slotZ);
        }

        return canonical(transformer, slotX, slotZ);
    }

    // The same fold with no centre to spare — for the squares whose every member is a neighbour.
    public static ChunkPos canonical(WorldLoopTransformer transformer, int chunkX, int chunkZ) {
        return new ChunkPos(transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
    }

    private C2meSeamFold() {
    }
}
