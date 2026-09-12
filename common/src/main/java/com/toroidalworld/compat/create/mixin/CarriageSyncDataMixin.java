package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.entity.CarriageSyncData;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.world.phys.Vec3;

@Mixin(value = CarriageSyncData.class, remap = false)
public class CarriageSyncDataMixin {
    @WrapOperation(method = "approachVector",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 toroidal$approachTheShortWayRound(Vec3 target, Vec3 snapshot, Operation<Vec3> original) {
        return original.call(CreateClientFrame.nearestCopy(snapshot, target), snapshot);
    }
}
