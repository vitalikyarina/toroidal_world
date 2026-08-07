package com.toroidalworld.gen;

// The generator ids the mod registers its chunk-generator codecs under — the names persisted into every toroidal
// world's level.dat. Registration itself is loader glue and lives with the entrypoint wiring (WorldLoop); these
// constants are what the rest of the mod may name.
public final class WorldLoopGenerators {
    public static final String TOROIDAL_ID = "toroidal";
    public static final String TOROIDAL_FLAT_ID = "toroidal_flat";

    private WorldLoopGenerators() {
    }
}
