package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.SectionTracker;
import net.minecraft.server.level.ServerLevel;

@Mixin(SectionTracker.class)
public class SectionTrackerMixin implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Unique
    private @Nullable WorldLoopTransformer toroidal$boundTransformer;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    @WrapOperation(
            method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;offset(JIII)J"))
    private long toroidal$physicalNeighborSection(
            long sectionNode, int stepX, int stepY, int stepZ, Operation<Long> original) {
        long neighbor = original.call(sectionNode, stepX, stepY, stepZ);
        WorldLoopTransformer transformer = this.toroidal$transformer();
        return transformer == null ? neighbor : transformer.chunks.wrapSectionNode(neighbor);
    }

    @Unique
    private @Nullable WorldLoopTransformer toroidal$transformer() {
        WorldLoopTransformer transformer = this.toroidal$boundTransformer;
        if (transformer == null) {
            ServerLevel level = this.toroidal$level;
            if (level == null) {
                return null;
            }

            transformer = WorldLoopAttachments.transformerOf(level);
            this.toroidal$boundTransformer = transformer;
        }

        return transformer.isWrapped() ? transformer : null;
    }
}
