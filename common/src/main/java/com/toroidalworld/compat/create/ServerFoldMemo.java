package com.toroidalworld.compat.create;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;

public final class ServerFoldMemo {
    private record Entry(Object server, Object dimension, @Nullable WorldFold fold) {
    }

    private volatile @Nullable Entry entry;

    public @Nullable WorldFold of(Object server, Object dimension, Supplier<@Nullable WorldFold> resolve) {
        Entry known = entry;
        if (known != null && known.server() == server && known.dimension() == dimension) {
            return known.fold();
        }

        WorldFold fold = resolve.get();
        entry = new Entry(server, dimension, fold);
        return fold;
    }
}
