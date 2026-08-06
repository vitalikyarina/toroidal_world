package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

// Ten flight phases and one place they are all read: aiStep asks the current phase where to fly and works the whole of
// the flight out of that single value — the vertical climb it clamps, the aim it normalizes against its own facing, and
// the atan2 that turns the body. Across the seam a destination on the far side sends the dragon away from it, and the
// speed falls off with the distance it thinks it has to cover, so it wanders slowly in the wrong direction.
//
// Folding at that read is the rare case where one injection settles a whole subsystem: the holding pattern, the landing
// approach, the charge, the strafe run, the hover, the takeoff and the death spiral all put their destination through
// it. It reaches further than a destination handed in from outside, too — the hover phase records the dragon's own
// position and returns to it, and the tick-tail wrap moves the dragon out from under that record every time it crosses.
//
// The wing sweep is a second, separate reading. It shoves whatever its box caught away from the body centre, and the
// box query above it is already cut at the bounds, so the list can hold something a step away through the seam. Read
// raw, the shove aims the long way round and its 1/distance falloff scales a world-wide gap to no push at all — the
// dragon's own copy of the Entity.push defect EntityMixin fixed, which does not reach here because this is the
// apply-movement overload rather than the entity-to-entity one.
//
// A dragon only meets the seam at all if something put it outside the End — its fight is anchored at the origin and the
// End's floor size leaves the boundary some fifteen hundred blocks away — but a summon places one anywhere.
@Mixin(EnderDragon.class)
public class EnderDragonMixin {
    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;"
                            + "getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;"))
    private @Nullable Vec3 toroidal$flyTargetThroughSeam(@Nullable Vec3 targetLocation) {
        EnderDragon self = (EnderDragon) (Object) this;
        return targetLocation == null ? null : SeamAim.nearestTo(self, targetLocation);
    }

    // Landing and taking off, the head is pitched by how far the dragon still is from the egg — six over the rooted
    // distance, floored at one, so over the podium it saturates at a full tilt upward and eases to level as the dragon
    // climbs away. Read raw across the seam the distance is the width of the world, the tilt collapses to nothing, and
    // the dragon lands looking dead ahead. The same reading also drives the takeoff's own choice of where to fly, which
    // is taken from this vector.
    @WrapOperation(
            method = "getHeadLookVector",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D"))
    private double toroidal$eggDistanceThroughSeam(BlockPos eggPos, Position dragonPosition,
            Operation<Double> original) {
        return SeamRange.sqr((EnderDragon) (Object) this, Vec3.atCenterOf(eggPos), dragonPosition);
    }

    @ModifyExpressionValue(
            method = "knockBack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$shovedEntityX(double entityX) {
        return SeamAim.nearX((EnderDragon) (Object) this, entityX);
    }

    @ModifyExpressionValue(
            method = "knockBack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$shovedEntityZ(double entityZ) {
        return SeamAim.nearZ((EnderDragon) (Object) this, entityZ);
    }
}
