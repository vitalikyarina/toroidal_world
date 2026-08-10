package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.probe.ReseatProbe;
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
// two player predicates are targeted as the synthetic lambda$static$0/$3 (javap-verified for this vanilla build; a
// recompile that shifts them fails loudly at mixin apply). The SHEEP detector needs no range fold: its AABB entity
// query is already split at the seam by LevelMixin.
@Mixin(PlayerDetector.class)
public interface PlayerDetectorMixin {
    @WrapOperation(
            method = {"lambda$static$0", "lambda$static$3"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private static boolean toroidal$detectionRangeThroughSeam(BlockPos playerPos, Vec3i spawnerPos, double range,
            Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        return ReseatProbe.decided(player.level(), ReseatProbe.DETECTOR_RANGE,
                original.call(playerPos, spawnerPos, range),
                SeamRange.closerThan(player.level(), playerPos, spawnerPos, range));
    }

    @WrapMethod(method = "inLineOfSight")
    private static boolean toroidal$sightLineThroughSeam(Level level, Vec3 origin, Vec3 dest, Operation<Boolean> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null
                ? original.call(level, origin, dest)
                : original.call(level, origin, transformer.vectors.nearestCopy(origin, dest));
    }
}
