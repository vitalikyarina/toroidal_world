package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import com.toroidalworld.core.JomlVectors;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.world.level.Level;

public final class SpringSeamFrame {

    public static Vector3d seat(@Nullable Level level, Vector3dc own, Vector3d partner) {
        return seat(level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level), own, partner);
    }

    static Vector3d seat(@Nullable WorldFold fold, Vector3dc own, Vector3d partner) {
        if (fold == null) {
            return partner;
        }

        return JomlVectors.seat(fold, JomlVectors.read(own), partner);
    }

    private SpringSeamFrame() {
    }
}
