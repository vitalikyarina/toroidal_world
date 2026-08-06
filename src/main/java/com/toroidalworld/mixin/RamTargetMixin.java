package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.ai.behavior.RamTarget;

// The charge is aimed once, when it starts: the goat writes down the direction from its target to itself and keeps it
// for the whole run, using it to throw whatever it hits. That direction is a plain difference between two absolute
// positions, so across the seam it comes out reversed and the goat knocks its victim towards itself instead of away.
//
// Damage knockback in general is already folded on LivingEntity, but this one is the goat's own arithmetic, taken
// before the hit and stored, so nothing downstream can correct it. Each horizontal difference is folded the short way;
// the normalize and everything after stay vanilla's.
@Mixin(RamTarget.class)
public class RamTargetMixin {
    @ModifyArg(
            method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/goat/Goat;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V"),
            index = 0)
    private double toroidal$ramDirectionX(double deltaX, @Local(argsOnly = true) Goat body) {
        return SeamAim.foldX(body, deltaX);
    }

    @ModifyArg(
            method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/goat/Goat;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V"),
            index = 2)
    private double toroidal$ramDirectionZ(double deltaZ, @Local(argsOnly = true) Goat body) {
        return SeamAim.foldZ(body, deltaZ);
    }
}
