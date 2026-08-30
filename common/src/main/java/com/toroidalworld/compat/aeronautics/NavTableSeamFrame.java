package com.toroidalworld.compat.aeronautics;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class NavTableSeamFrame {
    public static Vec3 seatTarget(NavTableBlockEntity navTable, Vec3 target) {
        WorldFold fold = foldOf(navTable.getLevel());
        return fold == null ? target : fold.nearestCopy(navTable.getProjectedSelfPos(), target);
    }

    private static @Nullable WorldFold foldOf(@Nullable Level level) {
        return level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
    }

    private NavTableSeamFrame() {
    }
}
