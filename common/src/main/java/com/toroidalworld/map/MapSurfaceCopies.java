package com.toroidalworld.map;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class MapSurfaceCopies {
    private static Copies bound = Copies.NONE;

    public record Copies(int reach, BoundingBox painted) {
        public static final Copies NONE = new Copies(0, BoundingBox.infinite());

        public Copies {
            if (reach < 0) {
                throw new IllegalArgumentException("A copy reach is never negative, got " + reach);
            }
        }
    }

    public static Copies bind(Copies copies) {
        Copies previous = bound;
        bound = copies;
        return previous;
    }

    public static void restore(Copies previous) {
        bound = previous;
    }

    public static Copies current() {
        return bound;
    }

    private MapSurfaceCopies() {
    }
}
