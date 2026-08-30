package com.toroidalworld.compat.aeronautics;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.sable.SeamFrame;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class MergingGlueSeamFrame {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void control(BlockEntity controller, BlockEntity partner, Runnable original) {
        Level level = controller.getLevel();
        WorldFold fold = level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
        SubLevel own = Sable.HELPER.getContaining(controller);
        SubLevel other = Sable.HELPER.getContaining(partner);
        if (fold == null || own == null || other == null) {
            original.run();
            return;
        }

        Vec3 anchor = positionOf(own);
        probe(fold, anchor, positionOf(other));
        SeamFrame.run(level, () -> anchor, original);
    }

    private static void probe(WorldFold fold, Vec3 anchor, Vec3 rawPartner) {
        Vec3 seated = fold.nearestCopy(anchor, rawPartner);
        LOGGER.info("[aeronautics-compat] merging_glue_control_frame controller_x_blocks={} "
                        + "raw_partner_x_blocks={} seated_partner_x_blocks={} raw_gap_blocks={} "
                        + "seated_gap_blocks={} moved={}",
                anchor.x, rawPartner.x, seated.x, rawPartner.distanceTo(anchor), seated.distanceTo(anchor),
                seated.x != rawPartner.x || seated.z != rawPartner.z);
    }

    private static Vec3 positionOf(SubLevel subLevel) {
        var position = subLevel.logicalPose().position();
        return new Vec3(position.x(), position.y(), position.z());
    }

    private MergingGlueSeamFrame() {
    }
}
