package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.toroidalworld.compat.create.RelativeKeyFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(value = SteamEngineBlockEntity.class, remap = false)
public abstract class SteamEngineBlockEntityMixin {
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldGateKey(BlockPos shaftPos, Vec3i enginePos, Operation<BlockPos> original) {
        SteamEngineBlockEntity self = (SteamEngineBlockEntity) (Object) this;
        return RelativeKeyFold.shortWay(self.getLevel(), shaftPos, enginePos, original.call(shaftPos, enginePos));
    }
}
