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
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class SableConstraintJoin {
    record Anchors(Vector3dc first, Vector3dc second) {
    }

    public static PhysicsConstraintConfiguration<?> seat(ServerLevel level, PhysicsPipeline pipeline,
            @Nullable PhysicsPipelineBody bodyA, @Nullable PhysicsPipelineBody bodyB,
            PhysicsConstraintConfiguration<?> configuration) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(level);
        if (fold == null || bodyA == bodyB) {
            return configuration;
        }

        return SeamFrame.unbound(() -> {
            if (bodyA == null || bodyB == null) {
                return seatStaticAnchor(level, fold, bodyA == null, bodyA == null ? bodyB : bodyA, configuration);
            }

            shiftSmallerGroup(level, fold, pipeline, bodyA, bodyB, configuration);
            return configuration;
        });
    }

    private static PhysicsConstraintConfiguration<?> seatStaticAnchor(ServerLevel level, WorldFold fold,
            boolean staticIsFirst, @Nullable PhysicsPipelineBody body,
            PhysicsConstraintConfiguration<?> configuration) {
        if (body == null || body.isRemoved()) {
            return configuration;
        }

        Anchors anchors = anchorsOf(configuration);
        Vector3dc staticAnchor = staticIsFirst ? anchors.first() : anchors.second();
        Vec3 raw = new Vec3(staticAnchor.x(), staticAnchor.y(), staticAnchor.z());
        Vec3 bodyAnchor = SableBodyPose.anchorInWorld(body, staticIsFirst ? anchors.second() : anchors.first());
        if (bodyAnchor == null) {
            return configuration;
        }

        Vec3 seated = fold.nearestCopy(bodyAnchor, raw);
        if (seated.x == raw.x && seated.z == raw.z) {
            return configuration;
        }

        Vector3d anchor = new Vector3d(seated.x, seated.y, seated.z);
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || container.inBounds(anchor)) {
            return configuration;
        }

        return withAnchor(configuration, staticIsFirst, anchor);
    }

    private static void shiftSmallerGroup(ServerLevel level, WorldFold fold, PhysicsPipeline pipeline,
            PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB,
            PhysicsConstraintConfiguration<?> configuration) {
        if (bodyA.isRemoved() || bodyB.isRemoved()) {
            return;
        }

        SubLevelPhysicsSystem system = SubLevelPhysicsSystem.get(level);
        if (system == null) {
            return;
        }

        Anchors anchors = anchorsOf(configuration);
        Vec3 worldA = SableBodyPose.anchorInWorld(bodyA, anchors.first());
        Vec3 worldB = SableBodyPose.anchorInWorld(bodyB, anchors.second());
        if (worldA == null || worldB == null) {
            return;
        }

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

    static PhysicsConstraintConfiguration<?> withAnchor(PhysicsConstraintConfiguration<?> configuration,
            boolean first, Vector3dc anchor) {
        return switch (configuration) {
            case FixedConstraintConfiguration config -> first
                    ? new FixedConstraintConfiguration(anchor, config.pos2(), config.orientation())
                    : new FixedConstraintConfiguration(config.pos1(), anchor, config.orientation());
            case FreeConstraintConfiguration config -> first
                    ? new FreeConstraintConfiguration(anchor, config.pos2(), config.orientation())
                    : new FreeConstraintConfiguration(config.pos1(), anchor, config.orientation());
            case RotaryConstraintConfiguration config -> first
                    ? new RotaryConstraintConfiguration(anchor, config.pos2(), config.normal1(), config.normal2())
                    : new RotaryConstraintConfiguration(config.pos1(), anchor, config.normal1(), config.normal2());
            case GenericConstraintConfiguration config -> first
                    ? new GenericConstraintConfiguration(anchor, config.pos2(), config.orientation1(),
                            config.orientation2(), config.lockedAxes())
                    : new GenericConstraintConfiguration(config.pos1(), anchor, config.orientation1(),
                            config.orientation2(), config.lockedAxes());
        };
    }

    static Anchors anchorsOf(PhysicsConstraintConfiguration<?> configuration) {
        return switch (configuration) {
            case FixedConstraintConfiguration config -> new Anchors(config.pos1(), config.pos2());
            case FreeConstraintConfiguration config -> new Anchors(config.pos1(), config.pos2());
            case RotaryConstraintConfiguration config -> new Anchors(config.pos1(), config.pos2());
            case GenericConstraintConfiguration config -> new Anchors(config.pos1(), config.pos2());
        };
    }

    private SableConstraintJoin() {
    }
}
