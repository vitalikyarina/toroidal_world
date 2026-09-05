package com.toroidalworld.compat.sable.client;

import org.joml.Vector3d;

import com.toroidalworld.client.ClientFrame;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;

import net.minecraft.world.phys.Vec3;

public final class SableClientFrame {
    public static void reseat(Pose3dc pose) {
        if (!(pose instanceof Pose3d received)) {
            return;
        }

        Vector3d position = received.position();
        Vec3 raw = new Vec3(position.x, position.y, position.z);
        Vec3 seated = ClientFrame.nearestToPlayer(raw);
        if (seated == raw) {
            return;
        }

        position.set(seated.x, seated.y, seated.z);
    }

    private SableClientFrame() {
    }
}
