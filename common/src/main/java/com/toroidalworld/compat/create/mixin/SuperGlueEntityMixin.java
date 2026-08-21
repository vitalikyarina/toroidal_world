package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SuperGlueEntity.class, remap = false)
public class SuperGlueEntityMixin {
    @ModifyExpressionValue(
            method = "contains",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$foldBlockIntoTheBondFrame(Vec3 blockCenter) {
        Entity glue = (Entity) (Object) this;
        return CreateSeamFold.foldPointToBox(glue.level(), glue.getBoundingBox(), blockCenter);
    }
}
