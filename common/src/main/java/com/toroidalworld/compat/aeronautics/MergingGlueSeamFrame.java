package com.toroidalworld.compat.aeronautics;

import org.jspecify.annotations.Nullable;
import com.toroidalworld.compat.sable.SeamFrame;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class MergingGlueSeamFrame {

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
        SeamFrame.run(level, () -> anchor, original);
    }

    private static Vec3 positionOf(SubLevel subLevel) {
        var position = subLevel.logicalPose().position();
        return new Vec3(position.x(), position.y(), position.z());
    }

    private MergingGlueSeamFrame() {
    }
}
