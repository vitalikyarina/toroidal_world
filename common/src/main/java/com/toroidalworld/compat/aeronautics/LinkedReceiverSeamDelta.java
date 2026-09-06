package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.core.JomlVectors;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class LinkedReceiverSeamDelta {
    public static Vector3d fold(BlockEntity receiver, Vector3d target, Vector3dc current,
            Operation<Vector3d> original) {
        WorldFold fold = foldOf(receiver.getLevel());
        if (fold == null) {
            return original.call(target, current);
        }

        return fold(fold, target, current);
    }

    static Vector3d fold(WorldFold fold, Vector3d target, Vector3dc current) {
        Vec3 folded = fold.foldDelta(JomlVectors.read(current), JomlVectors.read(target));
        return JomlVectors.write(folded, target);
    }

    private static @Nullable WorldFold foldOf(@Nullable Level level) {
        return level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
    }

    private LinkedReceiverSeamDelta() {
    }
}
