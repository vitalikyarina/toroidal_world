package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.SeamDelta;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.simulated_team.simulated.content.blocks.redstone_magnet.SimMagnet;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class MagnetSeamDelta {
    public static Vector3d fold(SimMagnet magnet, Vector3d relative) {
        Level level = levelOf(magnet);
        WorldFold fold = level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
        if (fold == null) {
            return relative;
        }

        Vec3 raw = new Vec3(relative.x, relative.y, relative.z);
        Vec3 folded = SeamDelta.fold(fold, raw);
        return folded == raw ? relative : relative.set(folded.x, folded.y, folded.z);
    }

    private static @Nullable Level levelOf(SimMagnet magnet) {
        return magnet instanceof BlockEntity blockEntity ? blockEntity.getLevel() : null;
    }

    private MagnetSeamDelta() {
    }
}
