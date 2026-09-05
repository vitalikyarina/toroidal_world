package com.toroidalworld.core;

import java.util.List;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

public final class WorldFoldFixture {
    public static final WorldLoopBounds SQUARE = new WorldLoopBounds(-32, 32, -32, 32);
    public static final WorldLoopBounds ODD_BOUNDS = new WorldLoopBounds(-2, 3, -2, 3);
    public static final WorldLoopBounds UNEVEN_BOUNDS = new WorldLoopBounds(-48, 16, 0, 16);
    public static final WorldLoopBounds UNIT_BOUNDS = new WorldLoopBounds(0, 1, 0, 1);
    public static final WorldLoopBounds X_ONLY_BOUNDS =
            new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE);

    public static final WorldFold EVEN = new WorldLoopTransformer(SQUARE);
    public static final WorldFold ODD = new WorldLoopTransformer(ODD_BOUNDS);
    public static final WorldFold UNEVEN = new WorldLoopTransformer(UNEVEN_BOUNDS);
    public static final WorldFold UNIT = new WorldLoopTransformer(UNIT_BOUNDS);
    public static final WorldFold X_ONLY = new WorldLoopTransformer(X_ONLY_BOUNDS);

    public static final List<WorldFold> PER_AXIS = List.of(EVEN, ODD, UNEVEN, X_ONLY, WorldFolds.NOOP);

    private WorldFoldFixture() {
    }
}
