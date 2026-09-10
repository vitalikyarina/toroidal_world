package com.toroidalworld.engine.gen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.ChunkPos;

public final class TerrainMasks {
    private static final int WINDOW_CONSUMERS = 9;

    private record Held(TerrainMask mask, AtomicInteger remaining) {
    }

    private final Map<Long, Held> held = new ConcurrentHashMap<>();

    public void put(ChunkPos pos, TerrainMask mask) {
        this.held.put(pos.toLong(), new Held(mask, new AtomicInteger(WINDOW_CONSUMERS)));
    }

    @Nullable TerrainMask at(long key) {
        Held found = this.held.get(key);
        return found != null ? found.mask() : null;
    }

    void consumed(long key) {
        Held found = this.held.get(key);
        if (found != null && found.remaining().decrementAndGet() <= 0) {
            this.held.remove(key);
        }
    }
}
