package com.toroidalworld.compat.sable;

import java.util.List;

import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.sable.mixin.SubLevelAccessor;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public final class SablePoseFold {
    private static final double SECTION_REACH_BLOCKS = 1.0;
    private static final double VELOCITY_EPSILON_SQUARED = 1.0E-18;

    public static void reseat(SubLevelPhysicsSystem system, ServerSubLevel subLevel, Pose3d readback) {
        ServerLevel level = system.getLevel();
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(level);
        if (fold == null) {
            return;
        }

        PhysicsPipeline pipeline = system.getPipeline();
        List<PhysicsPipelineBody> group = SableConstraintGraph.groupOf(pipeline, subLevel);
        Vector3d centroid = new Vector3d();
        int counted = 0;
        for (PhysicsPipelineBody body : group) {
            Vector3dc position = positionOf(body, subLevel, readback);
            if (position != null) {
                centroid.add(position);
                counted++;
            }
        }

        centroid.div(counted);
        Vec3 centre = new Vec3(centroid.x, centroid.y, centroid.z);
        boolean centroidOverBounds = fold.isOver(centre);
        SableConstraintProbe.groupSpan(level, group, centre, centroidOverBounds);
        if (!centroidOverBounds) {
            return;
        }

        Vec3 folded = fold.fold(centre);
        Vector3d lap = new Vector3d(folded.x - centre.x, 0.0, folded.z - centre.z);
        shiftGroup(system, group, lap, subLevel, readback);
    }

    static void shiftGroup(SubLevelPhysicsSystem system, List<PhysicsPipelineBody> group, Vector3dc lap,
            @Nullable ServerSubLevel self, @Nullable Pose3d readback) {
        PhysicsPipeline pipeline = system.getPipeline();
        for (PhysicsPipelineBody body : group) {
            shift(system, pipeline, body, self, readback, lap);
        }

        SableBodyShift.fire(system.getLevel(), group, lap);
    }

    private static void shift(SubLevelPhysicsSystem system, PhysicsPipeline pipeline, PhysicsPipelineBody body,
            @Nullable ServerSubLevel self, @Nullable Pose3d readback, Vector3dc lap) {
        Vector3dc position = positionOf(body, self, readback);
        Quaterniondc orientation = orientationOf(body, self, readback);
        if (position == null || orientation == null) {
            return;
        }

        Vector3d target = new Vector3d(position).add(lap);
        Vector3d linearBefore = pipeline.getLinearVelocity(body, new Vector3d());
        Vector3d angularBefore = pipeline.getAngularVelocity(body, new Vector3d());
        pipeline.teleport(body, target, orientation);
        Vector3d linearLost = linearBefore.sub(pipeline.getLinearVelocity(body, new Vector3d()), new Vector3d());
        Vector3d angularLost = angularBefore.sub(pipeline.getAngularVelocity(body, new Vector3d()), new Vector3d());
        if (linearLost.lengthSquared() > VELOCITY_EPSILON_SQUARED || angularLost.lengthSquared() > VELOCITY_EPSILON_SQUARED) {
            pipeline.addLinearAndAngularVelocity(body, linearLost, angularLost);
        }

        if (body == self) {
            readback.position().set(target);
        }

        if (body instanceof ServerSubLevel subLevel) {
            ((SubLevelAccessor) subLevel).toroidal$lastPose().position().add(lap);
            subLevel.lastNetworkedPose().position().add(lap);
            subLevel.updateBoundingBox();
            uploadSections(system, pipeline, subLevel);
        }
    }

    private static void uploadSections(SubLevelPhysicsSystem system, PhysicsPipeline pipeline, ServerSubLevel subLevel) {
        ServerLevel level = system.getLevel();
        PhysicsChunkTicketManager tickets = system.getTicketManager();
        BoundingBox3d reach = new BoundingBox3d(subLevel.boundingBox());
        reach.expand(SECTION_REACH_BLOCKS, reach);
        BoundingBox3i chunks = reach.chunkBoundsFrom();
        for (int x = chunks.minX(); x <= chunks.maxX(); x++) {
            for (int z = chunks.minZ(); z <= chunks.maxZ(); z++) {
                LevelChunk chunk = level.getChunk(x, z);
                for (int y = chunks.minY(); y <= chunks.maxY(); y++) {
                    int index = level.getSectionIndexFromSectionY(y);
                    if (index < 0 || index >= level.getSectionsCount()) {
                        continue;
                    }

                    tickets.addSectionIfNotTracked(level, chunk.getSection(index), SectionPos.of(x, y, z), pipeline);
                }
            }
        }
    }

    private static @Nullable Vector3dc positionOf(PhysicsPipelineBody body, @Nullable ServerSubLevel self,
            @Nullable Pose3d readback) {
        if (body == self) {
            return readback.position();
        }

        if (body instanceof ServerSubLevel subLevel) {
            return subLevel.logicalPose().position();
        }

        return body instanceof BoxPhysicsObject box ? box.getPose().position() : null;
    }

    private static @Nullable Quaterniondc orientationOf(PhysicsPipelineBody body, @Nullable ServerSubLevel self,
            @Nullable Pose3d readback) {
        if (body == self) {
            return readback.orientation();
        }

        if (body instanceof ServerSubLevel subLevel) {
            return subLevel.logicalPose().orientation();
        }

        return body instanceof BoxPhysicsObject box ? box.getPose().orientation() : null;
    }

    private SablePoseFold() {
    }
}
