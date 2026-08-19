package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin {
    @Shadow
    @Final
    protected LevelHeightAccessor levelHeightAccessor;

    // The engine runs on its own thread and never changes level, so the transformer is read once from behind it.
    @Unique
    private @Nullable WorldLoopTransformer toroidal$transformer;

    @Inject(method = "checkBlock", at = @At("HEAD"), cancellable = true)
    private void toroidal$skipPhantomBlock(BlockPos pos, CallbackInfo ci) {
        if (toroidal$isPhantom(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            ci.cancel();
        }
    }

    @Inject(method = "propagateLightSources", at = @At("HEAD"), cancellable = true)
    private void toroidal$skipPhantomSources(ChunkPos pos, CallbackInfo ci) {
        if (toroidal$isPhantom(pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "setLightEnabled", at = @At("HEAD"), cancellable = true)
    private void toroidal$skipPhantomEnable(ChunkPos pos, boolean enable, CallbackInfo ci) {
        if (toroidal$isPhantom(pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "retainData", at = @At("HEAD"), cancellable = true)
    private void toroidal$skipPhantomRetain(ChunkPos pos, boolean retain, CallbackInfo ci) {
        if (toroidal$isPhantom(pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateSectionStatus", at = @At("HEAD"), cancellable = true)
    private void toroidal$skipPhantomSection(SectionPos pos, boolean sectionEmpty, CallbackInfo ci) {
        if (toroidal$isPhantom(pos.x(), pos.z())) {
            ci.cancel();
        }
    }

    @Inject(method = "queueSectionData", at = @At("HEAD"), cancellable = true)
    private void toroidal$skipPhantomSectionData(LightLayer layer, SectionPos pos, @Nullable DataLayer data,
            CallbackInfo ci) {
        if (toroidal$isPhantom(pos.x(), pos.z())) {
            ci.cancel();
        }
    }

    @Unique
    private boolean toroidal$isPhantom(ChunkPos pos) {
        WorldLoopTransformer transformer = toroidal$transformer();
        return transformer.isWrapped() && transformer.chunks.isOver(pos);
    }

    @Unique
    private boolean toroidal$isPhantom(int chunkX, int chunkZ) {
        WorldLoopTransformer transformer = toroidal$transformer();
        return transformer.isWrapped() && (transformer.chunks.x.isOver(chunkX) || transformer.chunks.z.isOver(chunkZ));
    }

    @Unique
    private WorldLoopTransformer toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = this.levelHeightAccessor instanceof Level level
                    ? WorldLoopAttachments.transformerOf(level)
                    : WorldLoopTransformer.NOOP;
        }

        return this.toroidal$transformer;
    }
}
