package com.toroidalworld.compat.create.client;

import java.util.function.Supplier;

import com.toroidalworld.map.MapSurfaceCopies;
import com.toroidalworld.map.MapSurfaceCopies.Copies;

public final class TrainMapSurface {
    public static <T> T showing(Copies copies, Supplier<T> render) {
        Copies previous = MapSurfaceCopies.bind(copies);
        try {
            return render.get();
        } finally {
            MapSurfaceCopies.restore(previous);
        }
    }

    private TrainMapSurface() {
    }
}
