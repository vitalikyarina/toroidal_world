package com.toroidalworld.compat.sable.client;

import org.joml.Vector3d;

import com.toroidalworld.client.engine.ClientFrame;
import com.toroidalworld.core.JomlVectors;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;

import net.minecraft.world.phys.Vec3;

public final class SableClientFrame {
    public static void reseat(Pose3dc pose) {
        if (!(pose instanceof Pose3d received)) {
            return;
        }

        Vector3d position = received.position();
        Vec3 raw = JomlVectors.read(position);
        Vec3 seated = ClientFrame.nearestToPlayer(raw);
        if (seated == raw) {
            return;
        }

        JomlVectors.write(seated, position);
    }

    private SableClientFrame() {
    }
}
