package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.phys.Vec3;

// The one place the game hands a mob effect to everyone standing around a point: shrieker Darkness at 40 blocks, the
// warden's own darkness pulse at 20, and elder guardian mining fatigue at 50. All three read the reach off a bare Vec3,
// so the seam is a wall none of them cross — a player two blocks from a shrieking sculk stays lit, and a guardian's
// fatigue stops at the edge of the world instead of at fifty blocks.
//
// Folded here rather than at each of the three callers, which is where the whole failure class already meets: they
// differ only in the radius they pass. The gate itself sits in the getPlayers predicate, a lambda that captures the
// point and the radius but not the level; the player it tests carries the level instead.
@Mixin(MobEffectUtil.class)
public class MobEffectUtilMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$effectRadiusThroughSeam(Vec3 origin, Position playerPosition, double radius,
            Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        return SeamRange.closerThan(player, origin, playerPosition, radius);
    }
}
