package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.c2me.C2meSeamFold;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessibleChunkSending;
import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;

@Mixin(ServerAccessibleChunkSending.class)
public class ServerAccessibleChunkSendingMixin {
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.STATIC_CACHE_2D_CREATE))
    @TargetHandler(
            mixin = "com.ishland.c2me.notickvd.mixin.MixinServerAccessibleChunkSending",
            name = "upgradeToThis")
    private StaticCache2D<GenerationChunkHolder> toroidal$foldRegionSlots(
            int centerX,
            int centerZ,
            int range,
            StaticCache2D.Initializer<GenerationChunkHolder> initializer,
            Operation<StaticCache2D<GenerationChunkHolder>> original,
            @Local(argsOnly = true) ChunkLoadingContext context) {
        return original.call(centerX, centerZ, range,
                C2meSeamFold.foldingInitializer(context, centerX, centerZ, initializer));
    }
}
