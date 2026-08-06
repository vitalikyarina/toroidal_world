package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;

// The same structure as a chunk on the other side of the seam must see it: its pieces moved by the whole world widths
// that separate the two frames. A start straddling the seam is asked for the same view by every chunk of its far half,
// so the view is built once and kept on the start itself — it costs a null field on every other start, and it dies with
// the start rather than living in a cache nothing ever prunes.
public interface FramedStructureStart {
    // The start as seen from a frame that many chunks away. The start itself for a zero delta; null only if the copy
    // could not be built.
    @Nullable StructureStart toroidal$framedBy(WorldGenLevel level, int deltaChunkX, int deltaChunkZ);
}
