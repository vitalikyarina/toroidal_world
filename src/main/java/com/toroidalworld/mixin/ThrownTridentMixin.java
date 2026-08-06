package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.Vec3;

// Loyalty steers the trident home by the raw difference between the owner's eyes and where the trident is. Across the
// seam that difference points away from the owner and carries a world of magnitude; the direction survives the
// normalize that follows, so the trident does not stall — it sets off on a lap of the whole world, phasing through
// everything on the way, and arrives about a minute later.
//
// The eye position is read twice in the same tick and both readings need the same copy: the other is the check that
// absorbs a trident which has caught up with a non-player owner, and it measures with Vec3.distanceTo, which
// EntityMixin's fold does not reach. Folding the reading itself rather than either thing derived from it settles both.
@Mixin(ThrownTrident.class)
public class ThrownTridentMixin {
    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$ownerEyeThroughSeam(Vec3 ownerEye) {
        return SeamAim.nearestTo((ThrownTrident) (Object) this, ownerEye);
    }
}
