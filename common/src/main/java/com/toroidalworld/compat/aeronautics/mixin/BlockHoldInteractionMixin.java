package com.toroidalworld.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.aeronautics.HoldInteractionSeamDistance;

import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;

import net.minecraft.world.entity.player.Player;

@Mixin(value = BlockHoldInteraction.class, remap = false)
public class BlockHoldInteractionMixin {
    @WrapOperation(
            method = {
                    "inInteractionRange(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/Position;D)Z",
                    "inInteractionRange(Lnet/minecraft/world/entity/player/Player;Lorg/joml/Vector3dc;D)Z"},
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector3d;distanceSquared(DDD)D"))
    private static double toroidal$eyeDistanceThroughSeam(Vector3d target, double eyeX, double eyeY, double eyeZ,
            Operation<Double> original, @Local(argsOnly = true) Player player) {
        return HoldInteractionSeamDistance.sqrToEye(target, eyeX, eyeY, eyeZ, player, original);
    }
}
