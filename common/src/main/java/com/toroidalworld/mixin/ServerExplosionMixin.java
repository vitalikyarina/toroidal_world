package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.entity.SeamAim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
    @ModifyVariable(
            method = "getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F",
            at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$exposureCentreThroughSeam(Vec3 centre, @Local(argsOnly = true) Entity entity) {
        return SeamAim.nearestTo(entity, centre);
    }

    @ModifyExpressionValue(
            method = { "hurtEntities()V", "hurtEntities(Ljava/util/List;)V" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"),
            require = 1)
    private Vec3 toroidal$knockbackDirectionThroughSeam(Vec3 delta, @Local Entity entity) {
        return SeamAim.foldDelta(entity, delta);
    }
}
