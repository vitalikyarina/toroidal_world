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

// Chunks past the bounds are phantoms: vanilla keeps holders for them because it needs generation neighbours, but their
// generation is cancelled, so they hold no blocks. Tracking already refuses to send them — a phantom would arrive under
// the coordinate the real chunk on the other side uses and overwrite it. The light engine was the one place still
// treating them as real.
//
// A chunk with no blocks is a chunk open to the sky at every height, so the engine lights a phantom column to 15 all the
// way to the bottom. Then the wrap carries that into the world: the column one block outside the boundary spills into
// the real edge column, which is why an ocean at the seam is lit at a depth where it should be black, shedding one level
// per block inland.
//
// So the engine is told the world ends where it ends: every entry point that names a position refuses one outside the
// bounds. Refusing rather than wrapping matches what tracking does, and wrapping would only relight real chunks over and
// over for no gain.
//
// It has to be this class and not ThreadedLevelLightEngine, which is where the work is actually scheduled. Guarding
// there would skip the scheduling too and read as the tidier place, but half of vanilla's own calls never pass through
// those overrides: lightChunk and initializeLight reach straight past them with super.propagateLightSources,
// super.setLightEnabled and super.updateSectionStatus — and lightChunk is the initial lighting of every chunk. The base
// class is the one point all of them funnel through.
//
// Client levels are untouched: their transformer is NOOP, and that is correct, because the client is told the world
// is infinite and must keep lighting the chunks it holds at those very coordinates.
@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin {
    @Shadow
    @Final
    protected LevelHeightAccessor levelHeightAccessor;

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

    // The engine runs on its own thread and never changes level, so the transformer is read once from the level behind
    // it. LevelLightEngine.EMPTY carries a bare LevelHeightAccessor rather than a Level, hence the check.
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
