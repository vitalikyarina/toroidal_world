package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class LinkedReceiverSeamDelta {
    public static Vector3d fold(BlockEntity receiver, Vector3d target, Vector3dc current,
            Operation<Vector3d> original) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(receiver.getLevel());
        if (fold == null) {
            return original.call(target, current);
        }

        Vec3 folded = fold.foldDelta(
                new Vec3(current.x(), current.y(), current.z()),
                new Vec3(target.x, target.y, target.z));
        return target.set(folded.x, folded.y, folded.z);
    }

    private LinkedReceiverSeamDelta() {
    }
}
