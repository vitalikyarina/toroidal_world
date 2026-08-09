package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.ai.goal.LlamaFollowCaravanGoal;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.phys.Vec3;

// A caravan llama walks the difference to the llama ahead of it, scaled to close the gap to two blocks. The gap comes
// from distanceTo and is already folded, so the step is the right length — and the direction, taken raw from the two
// absolute positions, is the opposite one. That is the worst of both: across the seam every member correctly measures
// the one ahead as two steps away and confidently walks most of a world the other way, so the caravan tears itself
// apart at the boundary.
//
// Folding the difference leaves the normalize and the scale vanilla derives from it untouched. The point the goal then
// hands to the navigation may sit just past the bounds, which is exactly where the llama should be walking; the
// navigation takes every target as the copy nearest the mob and keeps it there.
@Mixin(LlamaFollowCaravanGoal.class)
public class LlamaFollowCaravanGoalMixin {
    @Shadow
    @Final
    public Llama llama;

    @WrapOperation(
            method = "tick",
            at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$caravanDeltaThroughSeam(double x, double y, double z, Operation<Vec3> original) {
        return SeamAim.foldDelta(this.llama, original.call(x, y, z));
    }
}
