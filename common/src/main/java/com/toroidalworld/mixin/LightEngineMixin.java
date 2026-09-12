package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
