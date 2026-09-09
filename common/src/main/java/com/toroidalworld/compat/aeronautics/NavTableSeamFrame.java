package com.toroidalworld.compat.aeronautics;

import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.fold.NearestCopy;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;

import net.minecraft.world.phys.Vec3;

public final class NavTableSeamFrame {
    public static Vec3 seatTarget(NavTableBlockEntity navTable, Vec3 target) {
        return NearestCopy.toward(WorldLoopAttachments.wrappedTransformerOfReader(navTable.getLevel()),
                navTable.getProjectedSelfPos(), target);
    }

    private NavTableSeamFrame() {
    }
}
