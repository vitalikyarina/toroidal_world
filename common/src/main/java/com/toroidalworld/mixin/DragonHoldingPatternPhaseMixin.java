package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import net.minecraft.world.phys.Vec3;

@Mixin(DragonHoldingPatternPhase.class)
public class DragonHoldingPatternPhaseMixin {
    @WrapOperation(
            method = "findNewTarget",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_DIST_TO_CENTER_SQR))
    private double toroidal$eggDistanceThroughSeam(BlockPos eggPos, Position playerPosition,
            Operation<Double> original) {
        return SeamRange.sqr(((DragonPhaseAccessor) this).toroidal$dragon(), Vec3.atCenterOf(eggPos), playerPosition);
    }
}
