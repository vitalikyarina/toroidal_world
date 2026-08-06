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

// The shared base of both light engines, on its own thread. Two things here have to know about the seam: the light read
// asked for a position past the bounds (a spot on the far side answers as open sky or void — this is what the AI reads),
// and every block-state read the engine makes through its chunk source. Wrapping the read position replaces the
// LevelMixin.getRawBrightness stopgap from the inside, covering every path through the engine, not only Level's.
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

    // Propagation already wraps its target node, so no read should ever escape the bounds; this is the backstop that
    // guarantees it — a read past the bounds would otherwise find an ungenerated chunk and answer as bedrock.
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

    // Resolved once and shared with the block and sky engines, which are LightEngines and reach it through
    // TransformerHolder rather than each caching their own. This runs on the light thread's hot path, so the field is
    // deliberately not volatile: resolution is idempotent — transformerOf hands back the level's one attachment
    // instance — so a race can only cost a repeated lookup, never a second transformer.
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
