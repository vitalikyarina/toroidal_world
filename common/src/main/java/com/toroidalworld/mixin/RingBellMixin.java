package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.probe.ReseatProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.RingBell;

// Ringing the bell is a bare "am I within three blocks of the meeting point?" with nothing behind it — no walk, no
// retry, no second branch. What walked the villager here stops it as soon as it is within six of the bell, which past
// the seam can leave it standing on the far side of the boundary: the gate then reads a world wide and the bell is
// never rung. The bell is what gathers the village at day's end and what sounds the raid alarm, so a village that
// straddles the seam loses both.
@Mixin(RingBell.class)
public class RingBellMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private static boolean toroidal$bellReachThroughSeam(BlockPos bellPos, Vec3i bodyPos, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.BELL_REACH,
                original.call(bellPos, bodyPos, distance),
                SeamRange.closerThan(body, bellPos, bodyPos, distance));
    }
}
