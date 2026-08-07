package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;

// A spider that cannot find a path climbs at its destination directly, and this is where it decides it has got there:
// once within its own width of the remembered position, the destination is dropped and the climb ends. The position is
// stored before the path search rather than taken from the path, so it is never unwrapped toward the mob the way path
// targets are — it stays where the caller named it, in the world.
//
// Across the seam the spider is therefore never there. The destination is never dropped, and every tick it is fed back
// into the move control as somewhere to head for; the move control folds it, so the spider does climb the right way,
// and then goes on climbing at a wall it is already on.
//
// The second reading is the same question with the height thrown away, for a spider hanging above its destination. Both
// live in one method and fold identically, so one handler answers both.
@Mixin(WallClimberNavigation.class)
public class WallClimberNavigationMixin {
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$climbTargetReachThroughSeam(BlockPos climbPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(((PathNavigationAccessor) this).toroidal$mob(), climbPos, bodyPosition,
                distance);
    }
}
