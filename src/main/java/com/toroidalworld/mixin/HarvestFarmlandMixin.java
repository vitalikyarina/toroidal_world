package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.npc.villager.Villager;

// The plot is chosen from the block cube around the farmer, so at the moment it is picked the two positions share one
// frame and no seam can lie between them. What separates them is time: the plot is remembered across a whole working
// session, and a farmer working the ground at the boundary steps over it and is wrapped to the other side of the world.
//
// From that tick on the gate reads its own plot a world out, and the gate is the entire body of the tick — nothing is
// harvested, nothing replanted, no further plot is chosen. The farmer stands over the crop until the session times out
// and starts again, which near the seam is every session.
@Mixin(HarvestFarmland.class)
public class HarvestFarmlandMixin {
    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/entity/npc/villager/Villager;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$farmlandReachThroughSeam(BlockPos farmlandPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Villager body) {
        return SeamRange.closerToCenterThan(body, farmlandPos, bodyPosition, distance);
    }
}
