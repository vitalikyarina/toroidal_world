package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import net.minecraft.world.phys.Vec3;

// While it circles, the dragon decides how often to break off and strafe a player, and the odds are set by how far that
// player stands from the egg: close to the podium is dangerous, far out is left alone. The distance is a raw difference
// between two positions neither of which is the dragon — the egg from the fight's origin, the player where it stands.
//
// Across the seam it reads the width of the world, which divided down makes that roll all but impossible. The strafe
// does not stop outright — the choice is two rolls taken together and the second counts only the crystals still alive —
// so what the seam takes away is the boost that ought to single out whoever stands closest to the podium.
@Mixin(DragonHoldingPatternPhase.class)
public class DragonHoldingPatternPhaseMixin {
    @WrapOperation(
            method = "findNewTarget",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D"))
    private double toroidal$eggDistanceThroughSeam(BlockPos eggPos, Position playerPosition,
            Operation<Double> original) {
        return SeamRange.sqr(((DragonPhaseAccessor) this).toroidal$dragon(), Vec3.atCenterOf(eggPos), playerPosition);
    }
}
