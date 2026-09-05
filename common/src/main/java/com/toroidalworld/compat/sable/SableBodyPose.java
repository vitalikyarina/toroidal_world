package com.toroidalworld.compat.sable;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import net.minecraft.world.phys.Vec3;

final class SableBodyPose {
    static @Nullable Pose3dc of(PhysicsPipelineBody body) {
        return switch (body) {
            case ServerSubLevel subLevel -> subLevel.logicalPose();
            case BoxPhysicsObject box -> box.getPose();
            default -> null;
        };
    }

    static @Nullable Vec3 anchorInWorld(PhysicsPipelineBody body, Vector3dc anchor) {
        Pose3dc pose = of(body);
        if (pose == null) {
            return null;
        }

        Vector3d world = pose.transformPosition(anchor, new Vector3d());
        return new Vec3(world.x, world.y, world.z);
    }

    private SableBodyPose() {
    }
}
