package com.toroidalworld.compat.aeronautics;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class DiagramSeamBox {
    public static AABB foldTowardPlayer(Player player, AABB box) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(player.level());
        return fold == null ? box : fold.foldBox(player.position(), box).value();
    }

    private DiagramSeamBox() {
    }
}
