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

// The tempted animals that do not path aim at a point drawn at random between themselves and the player holding the
// food, so that they drift in rather than lock on. The difference they draw it from is raw, and this one does not
// merely point the wrong way: it is scaled by a random fraction before it reaches the move control, and a fraction of a
// world is not the whole multiple of one that the wanted-position fold knows how to undo. What comes out is a fresh
// arbitrary point every tick, so the animal jitters on the spot instead of following the food at all.
//
// The difference is folded the moment it is taken, which leaves the random draw and the move control exactly as they
// were. The fold is asked of the player rather than the mob: this goal holds its mob only through the enclosing
// instance, and a difference needs no reference point to be taken the short way — the entity is here to name the level.
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
