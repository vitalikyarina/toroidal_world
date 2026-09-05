package com.toroidalworld.compat;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;

public final class CompatFoldFixture {
    public static final int WORLD_CHUNKS = 16;
    public static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    public static final int SKEW_CHUNKS = 4;
    public static final int MIRROR_LINE_CHUNK = 3;
    public static final int MIRROR_LINE_BLOCKS = MIRROR_LINE_CHUNK * 16;

    public static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);
    public static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-WORLD_CHUNKS, WORLD_CHUNKS), AxisBounds.Unbounded.INSTANCE);

    public static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    public static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    public static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    public static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));
    public static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(X_ONLY));
    public static final WorldFold DECK_CYLINDER = new DeckGroupFold(FlatShape.cylinder(X_ONLY));

    private CompatFoldFixture() {
    }
}
