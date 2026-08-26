package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.phys.Vec3;

@Mixin(PlayerDetector.class)
public interface PlayerDetectorMixin {
    @WrapOperation(
            method = {"lambda$static$1", "lambda$static$4"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private static boolean toroidal$detectionRangeThroughSeam(BlockPos playerPos, Vec3i spawnerPos, double range,
            Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        return SeamRange.closerThan(player.level(), playerPos, spawnerPos, range);
    }

    @WrapMethod(method = "inLineOfSight")
    private static boolean toroidal$sightLineThroughSeam(Level level, Vec3 origin, Vec3 dest, Operation<Boolean> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null
                ? original.call(level, origin, dest)
                : original.call(level, origin, transformer.nearestCopy(origin, dest));
    }
}
