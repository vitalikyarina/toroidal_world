package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.probe.ReseatProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.SocializeAtBell;

// Villagers only strike up a conversation while gathered at the bell, within four blocks of the meeting point. The
// meeting point is remembered where it stands and the villager is wrapped, so a village straddling the seam has the
// half on the far side judged to be elsewhere: they never take each other as an interaction target, never turn to face
// one another, and the gossip that travels along those meetings stops at the boundary.
@Mixin(SocializeAtBell.class)
public class SocializeAtBellMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$meetingPointReachThroughSeam(BlockPos meetingPos, Position bodyPosition,
            double distance, Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.MEETING_POINT_REACH,
                original.call(meetingPos, bodyPosition, distance),
                SeamRange.closerToCenterThan(body, meetingPos, bodyPosition, distance));
    }
}
