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

// The sibling of ChunkTracker on the section grid, and the graph that answers "how many sections to the nearest
// village": a flood fill whose only sources are the sections holding village POIs, spreading up to six sections out.
// Both of its neighbour walks — the spread (checkNeighborsAfterUpdate) and the recompute (getComputedLevel) — enumerate
// the twenty-six raw offsets around the node through one SectionPos.offset call, in unbounded space. Past the bounds
// that names a section the tracker has never heard of, so the aura stops dead at the seam instead of continuing onto
// the ground on the other side.
//
// A village against the seam still reads 0 on both halves, because a section is a source only by holding POIs of its
// own; what is lost is the skirt around it. Everything downstream measures the world by that skirt — Bad Omen starting
// a raid, the veto on spawning a pillager patrol near a village, an iron golem's stroll, the villager AI that decides
// whether a villager is home — so five blocks across the seam from a village is, to all of them, open wilderness.
//
// The physical key substitutes the raw one exactly as in ChunkTrackerMixin, and for the same reason: one physical
// section must carry one live key, or the same ground holds levels under two representations and neither is complete.
// Vanilla's own guards then do the rest against the only key that is live — a neighbour that folds onto the node reads
// as the self cell and becomes the source sentinel, and the level is read from where it is actually stored.
//
// Only Y is left alone. Sections stack vertically and nothing loops there; folding it would join the ceiling to the
// bedrock.
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

    // Resolved on first use and kept, exactly as the chunk tracker does it: a tracker belongs to one level for its whole
    // life, and a level's transformer is decided once by its generator. An unbound tracker answers null without storing
    // anything — memoizing that would pin the graph to an unwrapped world for good.
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
