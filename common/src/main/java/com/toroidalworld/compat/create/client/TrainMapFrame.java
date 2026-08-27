package com.toroidalworld.compat.create.client;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.compat.trainmap.TrainMapRenderer;
import com.toroidalworld.compat.create.CreateTrackFold;
import com.toroidalworld.core.WorldFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class TrainMapFrame {
    private static boolean bound;

    private static @Nullable WorldFold frame;

    public static void during(@Nullable ResourceKey<Level> drawnDimension, Runnable pass) {
        during(drawnDimension, () -> {
            pass.run();
            return null;
        });
    }

    public static <T> T during(@Nullable ResourceKey<Level> drawnDimension, Supplier<T> pass) {
        boolean previouslyBound = bound;
        WorldFold previousFrame = frame;
        frame = resolve(drawnDimension);
        bound = true;
        try {
            return pass.get();
        } finally {
            bound = previouslyBound;
            frame = previousFrame;
        }
    }

    static @Nullable WorldFold current() {
        return bound ? frame : resolve(TrainMapRenderer.INSTANCE.trackingDim);
    }

    private static @Nullable WorldFold resolve(@Nullable ResourceKey<Level> drawnDimension) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }

        if (drawnDimension != null && drawnDimension != level.dimension()) {
            return null;
        }

        return CreateTrackFold.transformerOf(level, null);
    }

    private TrainMapFrame() {
    }
}
