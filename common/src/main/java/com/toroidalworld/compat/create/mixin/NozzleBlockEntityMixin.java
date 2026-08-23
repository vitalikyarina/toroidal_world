package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = NozzleBlockEntity.class, remap = false)
public class NozzleBlockEntityMixin {
    @WrapOperation(
            method = {"tick", "lazyTick", "canSee"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$foldEntityPosition(Entity entity, Operation<Vec3> original) {
        Vec3 raw = original.call(entity);
        NozzleBlockEntity self = (NozzleBlockEntity) (Object) this;
        Vec3 center = Vec3.atCenterOf(self.getBlockPos());
        return CreateSeamFold.foldPoint(self.getLevel(), center, raw);
    }
}
