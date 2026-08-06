package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.phys.Vec3;

// Every item a mob throws at somebody leaves through here — a villager's gift, a trade handed to another villager, an
// allay setting down what it fetched, a copper golem returning a tool, a piglin's half of a barter. The item is spawned
// at the thrower, which is right, and then aimed by a plain difference from the thrower to the target, which across the
// seam carries the whole world's magnitude with the wrong sign: normalizing it hands back a unit vector pointing the
// exact opposite way, so the gift is flung away from the player standing next to it.
//
// This is the difference itself, not a range test, so none of the distance folds reach it — a villager may now decide
// correctly that its hero is three blocks away and still throw to the far side of the world.
//
// The target becomes its copy nearest the thrower before vanilla subtracts, which is the one primitive all six callers
// share; the arithmetic after it, and the velocity each caller mixes in, stay exactly as they were.
@Mixin(BehaviorUtils.class)
public class BehaviorUtilsMixin {
    @WrapOperation(
            method = "throwItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$throwDirectionThroughSeam(Vec3 targetPos, Vec3 throwerPos, Operation<Vec3> original,
            @Local(argsOnly = true) LivingEntity thrower) {
        return original.call(SeamAim.nearestTo(thrower, targetPos), throwerPos);
    }
}
