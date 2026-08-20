package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Mixin(TemptGoal.ForNonPathfinders.class)
public class TemptGoalForNonPathfindersMixin {
    @WrapOperation(
            method = "navigateTowards",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$temptDeltaThroughSeam(Vec3 eyePosition, Vec3 bodyPosition, Operation<Vec3> original,
            @Local(argsOnly = true) Player player) {
        return SeamAim.foldDelta(player, original.call(eyePosition, bodyPosition));
    }
}
