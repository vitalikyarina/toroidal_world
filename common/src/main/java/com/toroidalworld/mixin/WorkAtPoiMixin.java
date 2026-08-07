package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.npc.villager.Villager;

// Working is gated on standing at the workstation — 1.73 blocks, the diagonal of one block, which is as good as "on
// it". The job site is remembered as the position it occupies in the world and the villager is wrapped, so across the
// seam a villager that walked all the way to its own composter is measured a world from it and never starts; the
// profession stays, the trades never restock.
//
// Both readings are the same question asked at either end of the behaviour — may it begin, may it go on — so one
// handler serves them. The Villager-typed overrides carry synthetic bridges, which is why the targets are named by
// their full descriptor: a bridge holds no comparison to wrap.
@Mixin(WorkAtPoi.class)
public class WorkAtPoiMixin {
    @WrapOperation(
            method = {
                    "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/world/entity/npc/villager/Villager;)Z",
                    "canStillUse(Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/world/entity/npc/villager/Villager;J)Z" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$workstationReachThroughSeam(BlockPos jobSitePos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Villager body) {
        return SeamRange.closerToCenterThan(body, jobSitePos, bodyPosition, distance);
    }
}
