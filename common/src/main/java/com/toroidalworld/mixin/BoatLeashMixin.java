package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

// A boat does not use the shared elastic pull the rest of Leashable ends in — it writes its own, off a raw difference
// to the holder's absolute position. Across the seam that difference goes the long way round with a world of magnitude,
// so a boat towed through the boundary is shoved away from the holder it visibly hangs from, at speed.
//
// The holder is read at the copy nearest the boat and vanilla's own arithmetic runs on that. Only the holder's read
// carries Entity as its invoke owner; the boat's own position() compiles with Boat as the owner, so the two are told
// apart without an ordinal. A holder on this side is read as itself.
@Mixin(Boat.class)
public class BoatLeashMixin {
    @ModifyExpressionValue(
            method = "elasticRangeLeashBehaviour",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$holderPositionThroughSeam(Vec3 holderPosition) {
        Boat boat = (Boat) (Object) this;
        return SeamSteering.nearestCopy(boat, holderPosition);
    }
}
