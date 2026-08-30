package com.toroidalworld.compat.sable;

import java.util.List;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class SableConstraintJoin {
    private record Anchors(Vector3dc first, Vector3dc second) {
    }

    public static void seat(ServerLevel level, PhysicsPipeline pipeline, @Nullable PhysicsPipelineBody bodyA,
            @Nullable PhysicsPipelineBody bodyB, PhysicsConstraintConfiguration<?> configuration) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(level);
        if (fold == null || bodyA == null || bodyB == null || bodyA == bodyB) {
            return;
        }

        if (bodyA.isRemoved() || bodyB.isRemoved()) {
            return;
        }

        SubLevelPhysicsSystem system = SubLevelPhysicsSystem.get(level);
        if (system == null) {
            return;
        }

        Anchors anchors = anchorsOf(configuration);
        Vec3 worldA = worldAnchor(bodyA, anchors.first());
        Vec3 worldB = worldAnchor(bodyB, anchors.second());
        List<PhysicsPipelineBody> groupA = SableConstraintGraph.groupOf(pipeline, bodyA);
        List<PhysicsPipelineBody> groupB = SableConstraintGraph.groupOf(pipeline, bodyB);
        boolean movingIsB = groupB.size() <= groupA.size();
        Vec3 moving = movingIsB ? worldB : worldA;
        Vec3 nearest = fold.nearestCopy(movingIsB ? worldA : worldB, moving);
        double lapX = nearest.x - moving.x;
        double lapZ = nearest.z - moving.z;
        if (lapX == 0.0 && lapZ == 0.0) {
            return;
        }

        SablePoseFold.shiftGroup(system, movingIsB ? groupB : groupA, new Vector3d(lapX, 0.0, lapZ), null, null);
    }

    private static Anchors anchorsOf(PhysicsConstraintConfiguration<?> configuration) {
        return switch (configuration) {
            case FixedConstraintConfiguration config -> new Anchors(config.pos1(), config.pos2());
            case FreeConstraintConfiguration config -> new Anchors(config.pos1(), config.pos2());
            case RotaryConstraintConfiguration config -> new Anchors(config.pos1(), config.pos2());
            case GenericConstraintConfiguration config -> new Anchors(config.pos1(), config.pos2());
        };
    }

    private static Vec3 worldAnchor(PhysicsPipelineBody body, Vector3dc anchor) {
        if (body instanceof ServerSubLevel subLevel) {
            Vector3d world = subLevel.logicalPose().transformPosition(anchor, new Vector3d());
            return new Vec3(world.x, world.y, world.z);
        }

        return new Vec3(anchor.x(), anchor.y(), anchor.z());
    }

    private SableConstraintJoin() {
    }
}
