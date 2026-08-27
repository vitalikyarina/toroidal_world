package com.toroidalworld.compat.sable.mixin;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.toroidalworld.compat.sable.SeamFrame;

import dev.ryanhcode.sable.companion.math.Pose3dc;

@Mixin(value = Pose3dc.class, remap = false)
public interface Pose3dcMixin {
    @ModifyExpressionValue(
            method = {
                    "transformPosition(Lorg/joml/Vector3dc;Lorg/joml/Vector3d;)Lorg/joml/Vector3d;",
                    "transformPositionInverse(Lorg/joml/Vector3dc;Lorg/joml/Vector3d;)Lorg/joml/Vector3d;",
                    "bakeIntoMatrix"
            },
            at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/companion/math/Pose3dc;position()Lorg/joml/Vector3dc;"))
    private static Vector3dc toroidal$positionInTheEntityFrame(Vector3dc position) {
        Vector3dc shift = SeamFrame.shiftOf(position);
        return SeamFrame.isNoShift(shift) ? position : new Vector3d(position).add(shift);
    }
}
