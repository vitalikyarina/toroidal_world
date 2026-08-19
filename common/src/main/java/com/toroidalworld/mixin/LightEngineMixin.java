package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin implements TransformerHolder {
    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @Unique
    private WorldLoopTransformer toroidal$transformer;

    @ModifyVariable(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapLightValuePos(BlockPos pos) {
        WorldLoopTransformer transformer = toroidal$transformer();
        return transformer.isWrapped() ? transformer.blocks.wrap(pos) : pos;
    }

    @WrapOperation(
            method = "getChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LightChunkGetter;getChunkForLighting(II)Lnet/minecraft/world/level/chunk/LightChunk;"))
    private @Nullable LightChunk toroidal$wrapChunkForLighting(LightChunkGetter source, int chunkX, int chunkZ,
            Operation<@Nullable LightChunk> original) {
        WorldLoopTransformer transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return original.call(source, chunkX, chunkZ);
        }

        return original.call(source, transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
    }

    // Read once on the light thread and shared with both engines; resolution is idempotent, so the field is not volatile.
    @Override
    public WorldLoopTransformer toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            BlockGetter level = this.chunkSource.getLevel();
            this.toroidal$transformer = level instanceof Level realLevel
                    ? WorldLoopAttachments.transformerOf(realLevel)
                    : WorldLoopTransformer.NOOP;
        }

        return this.toroidal$transformer;
    }
}
