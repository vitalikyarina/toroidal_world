package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(EntityGetter.class)
public interface ServerEntityGetterMixin {
    @WrapOperation(
            method = "getNearbyPlayers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;contains(DDD)Z"))
    private static boolean toroidal$nearbyPlayerThroughSeam(AABB box, double x, double y, double z,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity source) {
        WorldFold transformer = ((TransformerSource) source).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(box, x, y, z);
        }

        return original.call(transformer.foldBox(new Vec3(x, y, z), box).value(), x, y, z);
    }
}
