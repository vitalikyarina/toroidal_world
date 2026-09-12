package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {
    @ModifyExpressionValue(method = "isUnderSpawnProtection", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/LevelData$RespawnData;pos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$nearestSpawnCopy(BlockPos spawnPos,
            @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos) {
        return WorldLoopAttachments.transformerOf(level).nearestCopy(pos, spawnPos);
    }
}
