package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

// Putting a curved track back into one frame, asked of the curve itself. A BezierConnection is a pair of absolute
// positions and nothing else — no level, no dimension — so it cannot answer where its own world ends until something
// that knows tells it, which is the same shape TrackNodeLocation has and the reason it carries a dimension field.
//
// What the curve owes: its two ends measured from the block entity that stores it. Create keeps the same curve twice,
// once on each end, and writes each copy as a delta from its own owner — so the frame is per copy, and a copy whose
// far end is named from the other side of the seam serialises a delta a world wide and derives a length to match.
public interface BezierCurveFold {
    // The level when the caller has one — a client level answers by the bounds the server sent, which is the only
    // reading available on a client with no integrated server. The dimension is remembered when given and kept when
    // null, so a curve stamped once stays foldable through every copy of itself that follows.
    void toroidal$foldCurve(@Nullable Level level, @Nullable ResourceKey<Level> dimension);
}
