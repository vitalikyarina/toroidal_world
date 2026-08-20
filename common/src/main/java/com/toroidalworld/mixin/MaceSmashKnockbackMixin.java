package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;

// The shockwave body is a lambda, so its name is whatever the running loader's mappings call it — Mojmap keeps
// javac's lambda$knockback$0 for NeoForge, intermediary gives it method_58409 for Fabric. Both are listed and
// defaultRequire = 1 takes whichever exists, the same dual-name pattern OreVeinSeamMixin uses.
@Mixin(MaceItem.class)
public class MaceSmashKnockbackMixin {
    @ModifyExpressionValue(
            method = {"lambda$knockback$0", "method_58409"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$smashPushThroughSeam(Vec3 direction, @Local(argsOnly = true) LivingEntity nearby) {
        return SeamAim.foldDelta(nearby, direction);
    }
}
