package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.SkyLightEngine;

@Mixin(SkyLightEngine.class)
public abstract class SkyLightEngineMixin {
    @WrapOperation(
            method = {"propagateIncrease", "propagateDecrease"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;offset(JLnet/minecraft/core/Direction;)J"))
    private long toroidal$wrapToNode(long node, Direction direction, Operation<Long> original) {
        long toNode = original.call(node, direction);
        WorldFold transformer = ((TransformerHolder) (Object) this).toroidal$transformer();
        return transformer.isWrapped() ? transformer.foldBlockNode(toNode) : toNode;
    }

    @WrapOperation(
            method = "getChunkSources",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LightChunkGetter;getChunkForLighting(II)Lnet/minecraft/world/level/chunk/LightChunk;"))
    private @Nullable LightChunk toroidal$wrapChunkSources(LightChunkGetter source, int chunkX, int chunkZ,
            Operation<@Nullable LightChunk> original) {
        WorldFold transformer = ((TransformerHolder) (Object) this).toroidal$transformer();
        if (!transformer.isWrapped()) {
            return original.call(source, chunkX, chunkZ);
        }

        long folded = transformer.foldChunkKey(ChunkPos.asLong(chunkX, chunkZ));
        return original.call(source, ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

}
