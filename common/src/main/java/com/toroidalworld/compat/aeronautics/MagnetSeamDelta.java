package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.JomlVectors;
import com.toroidalworld.core.SeamDelta;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.simulated_team.simulated.content.blocks.redstone_magnet.SimMagnet;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MagnetSeamDelta {
    public static Vector3d fold(SimMagnet magnet, Vector3d relative) {
        Level level = levelOf(magnet);
        return fold(level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level), relative);
    }

    static Vector3d fold(@Nullable WorldFold fold, Vector3d relative) {
        if (fold == null) {
            return relative;
        }

        return JomlVectors.write(SeamDelta.fold(fold, JomlVectors.read(relative)), relative);
    }

    private static @Nullable Level levelOf(SimMagnet magnet) {
        return magnet instanceof BlockEntity blockEntity ? blockEntity.getLevel() : null;
    }

    private MagnetSeamDelta() {
    }
}
