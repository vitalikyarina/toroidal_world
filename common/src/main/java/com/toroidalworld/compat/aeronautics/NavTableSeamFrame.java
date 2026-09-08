package com.toroidalworld.compat.aeronautics;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;

import net.minecraft.world.phys.Vec3;

public final class NavTableSeamFrame {
    public static Vec3 seatTarget(NavTableBlockEntity navTable, Vec3 target) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(navTable.getLevel());
        return fold == null ? target : fold.nearestCopy(navTable.getProjectedSelfPos(), target);
    }

    private NavTableSeamFrame() {
    }
}
