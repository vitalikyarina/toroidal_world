package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.compat.c2me.C2meSeamFold;
import com.toroidalworld.core.WorldFold;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.ishland.flowsched.scheduler.ItemHolder;

import net.minecraft.world.level.ChunkPos;

@Mixin(NewChunkStatus.class)
public class NewChunkStatusMixin {
    @WrapOperation(
            method = "relativeToAbsoluteDependencies",
            at = @At(value = "NEW", target = "(II)Lnet/minecraft/world/level/ChunkPos;"))
    private static ChunkPos toroidal$canonicalDependencyKey(
            int chunkX,
            int chunkZ,
            Operation<ChunkPos> original,
            @Local(argsOnly = true) ItemHolder<?, ?, ?, ?> holder) {
        WorldFold transformer = holder.getUserData().get() instanceof TransformerSource source
                ? source.toroidal$wrappedTransformer()
                : null;
        if (transformer == null) {
            return original.call(chunkX, chunkZ);
        }

        return C2meSeamFold.canonical(transformer, chunkX, chunkZ);
    }
}
