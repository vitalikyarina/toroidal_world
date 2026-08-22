package com.toroidalworld.map;

public final class MapSurfaceCopies {
    private static Copies bound = Copies.NONE;

    public record Copies(int rangeX, int rangeZ) {
        public static final Copies NONE = new Copies(0, 0);
    }

    public static Copies bind(int rangeX, int rangeZ) {
        Copies previous = bound;
        bound = new Copies(Math.max(0, rangeX), Math.max(0, rangeZ));
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
