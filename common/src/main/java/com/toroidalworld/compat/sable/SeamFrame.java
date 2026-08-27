package com.toroidalworld.compat.sable;

import java.util.function.Supplier;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public final class SeamFrame {
    private static final Vector3dc NO_SHIFT = new Vector3d();

    private static final ThreadLocal<@Nullable Binding> BOUND = new ThreadLocal<>();

    private record Binding(WorldFold fold, Supplier<Vec3> anchor) {
    }

    public static void run(LevelReader reader, Supplier<Vec3> anchor, Runnable body) {
        with(reader, anchor, () -> {
            body.run();
            return null;
        });
    }

    public static <R> R with(LevelReader reader, Supplier<Vec3> anchor, Supplier<R> body) {
        return with(WorldLoopAttachments.wrappedTransformerOfReader(reader), anchor, body);
    }

    public static <R> R with(@Nullable WorldFold fold, Supplier<Vec3> anchor, Supplier<R> body) {
        Binding previous = BOUND.get();
        rebind(fold == null ? null : new Binding(fold, anchor));
        try {
            return body.get();
        } finally {
            rebind(previous);
        }
    }

    public static boolean isBound() {
        return BOUND.get() != null;
    }

    public static Vector3dc shiftOf(Vector3dc posePosition) {
        Binding binding = BOUND.get();
        if (binding == null) {
            return NO_SHIFT;
        }

        Vec3 anchor = binding.anchor().get();
        Vec3 raw = new Vec3(posePosition.x(), posePosition.y(), posePosition.z());
        Vec3 nearest = binding.fold().nearestCopy(anchor, raw);
        double shiftX = nearest.x - raw.x;
        double shiftZ = nearest.z - raw.z;
        return shiftX == 0.0 && shiftZ == 0.0 ? NO_SHIFT : new Vector3d(shiftX, 0.0, shiftZ);
    }

    public static boolean isNoShift(Vector3dc shift) {
        return shift == NO_SHIFT;
    }

    private static void rebind(@Nullable Binding binding) {
        if (binding == null) {
            BOUND.remove();
        } else {
            BOUND.set(binding);
        }
    }

    private SeamFrame() {
    }
}
