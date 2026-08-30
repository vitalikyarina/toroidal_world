package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.entity.player.Player;

public final class HoldInteractionSeamDistance {
    public static double sqrToEye(Vector3d target, double eyeX, double eyeY, double eyeZ, Player player,
            Operation<Double> original) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(player.level());
        if (fold == null) {
            return original.call(target, eyeX, eyeY, eyeZ);
        }

        return fold.sqrDistance(eyeX, eyeY, eyeZ, target.x, target.y, target.z);
    }

    private HoldInteractionSeamDistance() {
    }
}
