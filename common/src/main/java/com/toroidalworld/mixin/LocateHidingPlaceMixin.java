package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.LocateHidingPlace;

// What a villager does when the bell rings or a raid starts: take the nearest house and get inside it. The POI search
// itself already reaches across the seam, so a house on the far side does come back — and is then thrown away by a raw
// range filter, leaving the villager to pick a random home anywhere in the wider radius or fall back on the one it
// sleeps in. The nearest shelter is the one candidate the filter is there to keep.
//
// The second reading decides whether to walk at all. Read raw it is always true, so a villager already standing inside
// its hiding place is given a walk target to it and shuffles about the doorway for the length of the raid.
@Mixin(LocateHidingPlace.class)
public class LocateHidingPlaceMixin {
    @WrapOperation(
            method = "*",
            require = 2,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"),
            expect = 2)
    private static boolean toroidal$hidingPlaceReachThroughSeam(BlockPos hidingPos, Position bodyPosition,
            double distance, Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerToCenterThan(body, hidingPos, bodyPosition, distance);
    }
}
