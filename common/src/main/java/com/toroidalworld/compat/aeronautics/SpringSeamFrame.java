package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SpringSeamFrame {

    public static Vector3d seat(@Nullable Level level, Vector3dc own, Vector3d partner) {
        WorldFold fold = level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
        if (fold == null) {
            return partner;
        }

        Vec3 origin = new Vec3(own.x(), own.y(), own.z());
        Vec3 raw = new Vec3(partner.x, partner.y, partner.z);
        Vec3 seated = fold.nearestCopy(origin, raw);
        if (seated.x == raw.x && seated.z == raw.z) {
            return partner;
        }

        return new Vector3d(seated.x, seated.y, seated.z);
    }

    private SpringSeamFrame() {
    }
}
