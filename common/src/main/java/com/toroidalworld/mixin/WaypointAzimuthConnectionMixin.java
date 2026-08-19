package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.WaypointTransmitter;

@Mixin(WaypointTransmitter.EntityAzimuthConnection.class)
public class WaypointAzimuthConnectionMixin {
    @Shadow
    @Final
    private ServerPlayer receiver;

    @WrapOperation(
            method = {"<init>", "update"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$sourcePositionThroughSeam(LivingEntity sourceEntity, Operation<Vec3> original) {
        Vec3 position = original.call(sourceEntity);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(sourceEntity.level());
        if (transformer == null) {
            return position;
        }

        return transformer.vectors.nearestCopy(this.receiver.position(), position);
    }
}
