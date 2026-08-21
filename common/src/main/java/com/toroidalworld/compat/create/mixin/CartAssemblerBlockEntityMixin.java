package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity", remap = false)
public class CartAssemblerBlockEntityMixin {
    @WrapOperation(method = "assemble",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$coupledCartInTheAssemblingFrame(Vec3 coupledPosition, Vec3 position,
            Operation<Vec3> original, @Local(argsOnly = true) Level world) {
        return original.call(CreateTrackFold.nearestCopy(world, position, coupledPosition), position);
    }
}
