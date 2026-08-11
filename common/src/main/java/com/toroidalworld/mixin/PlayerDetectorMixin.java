package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
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

// The trial spawner and the vault share this one primitive to ask "is a player here": a raw block distance, then a raw
// eye-line clip. Across the seam both read a world instead of a step — the spawner never wakes, Bad Omen never turns
// Trial Omen, the vault neither connects nor opens. The distance folds through the seam, and the clip aims at the
// player's copy nearest the spawner, which is where the player visually stands; block reads along the ray wrap on
// their way to a chunk, as they do for every other sight line. A same-side player is untouched.
//
// The range check lives inside the detector constants' predicate lambdas — there is no named method around it — so the
// target is named by the injection point instead of by the lambda: the wrapped call occurs in exactly the two player
// predicates and nowhere else in the class, and require pins that. A lambda's name is not something the loaded class
// carries — intermediary renames it to method_NNNNN — so naming one is an anchor Fabric cannot resolve. The SHEEP
// detector needs no range fold: its AABB entity query is already split at the seam by LevelMixin.
@Mixin(PlayerDetector.class)
public interface PlayerDetectorMixin {
    @WrapOperation(
            method = "*",
            require = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private static boolean toroidal$detectionRangeThroughSeam(BlockPos playerPos, Vec3i spawnerPos, double range,
            Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        return SeamRange.closerThan(player.level(), playerPos, spawnerPos, range);
    }

    @WrapMethod(method = "inLineOfSight")
    private static boolean toroidal$sightLineThroughSeam(Level level, Vec3 origin, Vec3 dest, Operation<Boolean> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null
                ? original.call(level, origin, dest)
                : original.call(level, origin, transformer.vectors.nearestCopy(origin, dest));
    }
}
