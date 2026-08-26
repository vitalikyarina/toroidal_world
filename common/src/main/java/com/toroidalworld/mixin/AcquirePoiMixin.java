package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;

@Mixin(AcquirePoi.class)
public class AcquirePoiMixin {
    @ModifyExpressionValue(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/pathfinder/Path;getTarget()Lnet/minecraft/core/BlockPos;"))
    private static @Nullable BlockPos toroidal$wrapClaimedPoi(@Nullable BlockPos target,
            @Local(argsOnly = true) ServerLevel level) {
        if (target == null) {
            return null;
        }
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? target : transformer.fold(target);
    }
}
