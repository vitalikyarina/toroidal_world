package com.toroidalworld.compat.aeronautics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RopeSeamFrame {
    public static Vec3 seatStart(RopeStrandHolderBehavior owner, RopeStrandHolderBehavior target, Vec3 start) {
        return seat(owner, target, start);
    }

    public static Vec3 seatTarget(RopeStrandHolderBehavior owner, RopeStrandHolderBehavior target, Vec3 point) {
        return seat(owner, target, point);
    }

    public static Vector3d seatAttachment(ServerRopeStrand strand, RopeAttachment attachment, ServerLevel level,
            Vector3d point) {
        if (attachment.subLevelID() != null) {
            return point;
        }

        WorldFold fold = foldOf(level);
        List<Vector3d> points = strand.getPoints();
        if (fold == null || points.isEmpty()) {
            return point;
        }

        Vector3d ownEnd = attachment.point() == RopeAttachmentPoint.END ? points.getLast() : points.getFirst();
        Vec3 anchor = new Vec3(ownEnd.x, ownEnd.y, ownEnd.z);
        Vec3 raw = new Vec3(point.x, point.y, point.z);
        Vec3 seated = fold.nearestCopy(anchor, raw);
        if (seated.x == raw.x && seated.z == raw.z) {
            return point;
        }

        return new Vector3d(seated.x, seated.y, seated.z);
    }

    public static void onGroupShifted(ServerLevel level, List<PhysicsPipelineBody> group, Vector3dc lap) {
        ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
        if (manager == null || manager.getAllStrands().isEmpty()) {
            return;
        }

        Set<UUID> shifted = new HashSet<>();
        for (PhysicsPipelineBody body : group) {
            if (body instanceof ServerSubLevel subLevel) {
                shifted.add(subLevel.getUniqueId());
            }
        }

        if (shifted.isEmpty()) {
            return;
        }

        SubLevelPhysicsSystem system = SubLevelPhysicsSystem.get(level);
        for (ServerRopeStrand strand : List.copyOf(manager.getAllStrands())) {
            UUID frame = frameBodyOf(strand);
            if (frame != null && shifted.contains(frame)) {
                reseat(system, strand, lap);
            }
        }
    }

    private static void reseat(@Nullable SubLevelPhysicsSystem system, ServerRopeStrand strand, Vector3dc lap) {
        List<Vector3d> points = strand.getPoints();
        if (points.isEmpty()) {
            return;
        }

        for (Vector3d point : points) {
            point.add(lap);
        }

        if (strand.isActive() && system != null) {
            system.removeObject(strand);
            system.addObject(strand);
        }
    }

    private static @Nullable UUID frameBodyOf(ServerRopeStrand strand) {
        UUID start = bodyOf(strand, RopeAttachmentPoint.START);
        return start != null ? start : bodyOf(strand, RopeAttachmentPoint.END);
    }

    private static @Nullable UUID bodyOf(ServerRopeStrand strand, RopeAttachmentPoint point) {
        RopeAttachment attachment = strand.getAttachment(point);
        return attachment == null ? null : attachment.subLevelID();
    }

    private static Vec3 seat(RopeStrandHolderBehavior owner, RopeStrandHolderBehavior target, Vec3 point) {
        Level level = owner.blockEntity.getLevel();
        WorldFold fold = foldOf(level);
        if (fold == null) {
            return point;
        }

        Vec3 ownerLocal = owner.getAttachmentPoint();
        Vec3 targetLocal = target.getAttachmentPoint();
        boolean ownerOnSubLevel = Sable.HELPER.getContaining(level, ownerLocal) != null;
        boolean targetOnSubLevel = !ownerOnSubLevel && Sable.HELPER.getContaining(level, targetLocal) != null;
        Vec3 anchor = Sable.HELPER.projectOutOfSubLevel(level, targetOnSubLevel ? targetLocal : ownerLocal);
        return fold.nearestCopy(anchor, point);
    }

    static @Nullable WorldFold foldOf(@Nullable Level level) {
        return level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
    }

    private RopeSeamFrame() {
    }
}
