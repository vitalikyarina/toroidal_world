package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.phys.Vec3;

// A shriek is answered by everyone standing within 16 blocks of the shrieker, not just the player who set it off: they
// share one warning level, and any of them still on cooldown vetoes the whole warning. The search that gathers them
// measures with a bare Vec3, which reads a neighbour two blocks away across the seam as a whole world away — so the
// warning stays private to the trigger, a warden takes four times as many shrieks to summon, and the cooldown veto that
// should have stopped the shriek never fires.
//
// The gate lives inside the getPlayers predicate, which compiles into a lambda of its own; the level is not captured
// there, but the player being tested is, and an entity already knows the level it stands in.
@Mixin(WardenSpawnTracker.class)
public class WardenSpawnTrackerMixin {
    @WrapOperation(
            method = "lambda$getNearbyPlayers$0",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$warningRangeThroughSeam(Vec3 playerPosition, Position shriekerOrigin,
            double distance, Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        return SeamRange.closerThan(player, playerPosition, shriekerOrigin, distance);
    }
}
