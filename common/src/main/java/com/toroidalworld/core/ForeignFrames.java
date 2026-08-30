package com.toroidalworld.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ForeignFrames {
    private static final List<ForeignFrameSource> SOURCES = new CopyOnWriteArrayList<>();

    public static void register(ForeignFrameSource source) {
        SOURCES.add(source);
    }

    public static List<ForeignFrame> of(Level level) {
        if (SOURCES.isEmpty()) {
            return List.of();
        }

        List<ForeignFrame> frames = new ArrayList<>(SOURCES.size());
        for (ForeignFrameSource source : SOURCES) {
            source.frameOf(level).ifPresent(frames::add);
        }

        return List.copyOf(frames);
    }

    public static Vec3 seatInWorld(Level level, Vec3 stored) {
        Vec3 seated = stored;
        for (ForeignFrameSource source : SOURCES) {
            seated = source.seatInWorld(level, seated);
        }

        return seated;
    }

    private ForeignFrames() {
    }
}
