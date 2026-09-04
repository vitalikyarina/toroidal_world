package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.npc.villager.Villager;

@Mixin(WorkAtPoi.class)
public class WorkAtPoiMixin {
    @WrapOperation(
            method = {
                    "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/world/entity/npc/villager/Villager;)Z",
                    "canStillUse(Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/world/entity/npc/villager/Villager;J)Z" },
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$workstationReachThroughSeam(BlockPos jobSitePos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Villager body) {
        return SeamRange.closerToCenterThan(body, jobSitePos, bodyPosition, distance);
    }
}
