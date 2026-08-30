package com.toroidalworld.compat.aeronautics;

import java.util.List;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RopeSeamFrame {
    public static Vec3 seatTarget(RopeStrandHolderBehavior holder, Vec3 target) {
        Level level = holder.blockEntity.getLevel();
        WorldFold fold = foldOf(level);
        if (fold == null) {
            return target;
        }

        Vec3 start = Sable.HELPER.projectOutOfSubLevel(level, holder.getAttachmentPoint());
        return fold.nearestCopy(start, target);
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

    static @Nullable WorldFold foldOf(@Nullable Level level) {
        return level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
    }

    private RopeSeamFrame() {
    }
}
