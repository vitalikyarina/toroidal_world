package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.animal.Bee;

// Every range a bee measures goes through one method on the bee, which folds the remembered position before comparing —
// except this one. Entering the hive asks the two-block question directly on the hive position, so it is the single
// reading that fold does not reach.
//
// The bee therefore flies home correctly, hovers at the entrance and never goes in: no honey delivered, no nectar
// cleared, and it stays out through the night and the rain.
@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeEnterHiveGoal")
public class BeeEnterHiveGoalMixin {
    // Taken from the constructor rather than shadowed off the goal's outer reference. That reference is javac's
    // this$0 — an artefact of the language, not a member any mapping set names — so on a loader that remaps the game
    // there is nothing to remap it to, and the shadow resolves to nothing at apply time. The constructor argument is
    // an ordinary parameter of an ordinary method, which every mapping set does carry.
    @Unique
    private Bee toroidal$bee;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V", at = @At("TAIL"))
    private void toroidal$captureBee(Bee bee, CallbackInfo ci) {
        this.toroidal$bee = bee;
    }

    @WrapOperation(
            method = "canBeeUse",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$hiveEntranceThroughSeam(BlockPos hivePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.toroidal$bee, hivePos, bodyPosition, distance);
    }
}
