package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.core.DeckTransformation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = AirCurrent.class, remap = false)
public class AirCurrentMixin {
    @WrapOperation(
            method = "tickAffectedEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private AABB toroidal$foldEntityBox(Entity entity, Operation<AABB> original) {
        AABB rawBox = original.call(entity);
        return toroidal$seatTransformation(entity.position()).apply(rawBox);
    }

    @WrapOperation(
            method = "tickAffectedEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$foldEntityPosition(Entity entity, Operation<Vec3> original) {
        Vec3 raw = original.call(entity);
        return toroidal$seatTransformation(raw).apply(raw);
    }

    @Unique
    private DeckTransformation toroidal$seatTransformation(Vec3 raw) {
        AirCurrent self = (AirCurrent) (Object) this;
        return CreateSeamFold.nearestCopyTransformation(self.source.getAirCurrentWorld(),
                Vec3.atCenterOf(self.source.getAirCurrentPos()), raw);
    }
}
