package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
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
    private WorldFold toroidal$transformer;

    @ModifyVariable(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapLightValuePos(BlockPos pos) {
        WorldFold transformer = toroidal$transformer();
        return transformer.isWrapped() ? transformer.fold(pos) : pos;
    }

    @WrapOperation(
            method = "getChunk",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.LIGHT_CHUNK_GETTER_GET_CHUNK_FOR_LIGHTING))
    private @Nullable LightChunk toroidal$wrapChunkForLighting(LightChunkGetter source, int chunkX, int chunkZ,
            Operation<@Nullable LightChunk> original) {
        WorldFold transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return original.call(source, chunkX, chunkZ);
        }

        long folded = transformer.foldChunkKey(ChunkPos.pack(chunkX, chunkZ));
        return original.call(source, ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

    // Read once on the light thread and shared with both engines; resolution is idempotent, so the field is not volatile.
    @Override
    public WorldFold toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            BlockGetter level = this.chunkSource.getLevel();
            this.toroidal$transformer = level instanceof Level realLevel
                    ? WorldLoopAttachments.transformerOf(realLevel)
                    : WorldFolds.NOOP;
        }

        return this.toroidal$transformer;
    }
}
