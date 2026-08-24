package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(targets = "com.simibubi.create.content.trains.track.TrackTargetingBehaviour", remap = false)
public class TrackTargetingMapMarkerMixin {
    @ModifyReturnValue(method = "getPositionForMapMarker", at = @At("RETURN"))
    private BlockPos toroidal$canonicalMapMarkerTarget(BlockPos target) {
        return ((BlockEntityBehaviour) (Object) this).getWorld() instanceof ServerLevel serverLevel
                ? CreateSeamFold.canonical(serverLevel, target)
                : target;
    }
}
