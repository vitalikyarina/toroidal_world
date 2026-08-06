package com.toroidalworld.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.FramedStructureStart;
import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

// A structure reference is a chunk key, and fetching the start back means loading the chunk that key names. The keys are
// stored canonically — one physical chunk, one live key — so a start reached across the seam is filed under
// its real position, which is a whole world from the chunk now asking for it.
//
// During generation that ask goes through the task's cache, which spans a small raw square around the chunk being
// generated and knows nothing of the far side: the canonical name is simply not in it ("Requested chunk unavailable").
// The name is therefore restated as the copy nearest that chunk before the read — the same fold ChunkGenerationTaskMixin
// applies to the slots themselves, so the two agree and the read lands on the real chunk it was always meant to reach.
//
// Only inside a WorldGenRegion. A lookup on a live level has no generating centre to fold around and needs none — the
// chunk source resolves wrapped positions there already.
@Mixin(StructureManager.class)
public class StructureManagerMixin {
    @Shadow
    @Final
    private LevelAccessor level;

    // The ChunkPos+Predicate overload is the Beardifier's fetch during worldgen. The folded getChunk below finds the
    // start across the seam, but hands back the live start with pieces in its own raw frame, a world away from the
    // asking chunk — every piece fails Beardifier's isCloseToChunk filter and the far half generates with no terrain
    // adaptation (no earth platform under village houses, buried structures exposed). So the same frame substitution
    // the placement fetch gets (ChunkGeneratorStructurePlacementMixin) is applied here: each start is restated in the
    // asking chunk's frame, from the same per-frame copy cache the placement already shares.
    @WrapMethod(
            method = "startsForStructure(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Predicate;)Ljava/util/List;")
    private List<StructureStart> toroidal$startsInTheAskingChunksFrame(ChunkPos pos, Predicate<Structure> matcher,
            Operation<List<StructureStart>> original) {
        List<StructureStart> starts = original.call(pos, matcher);
        if (!(this.level instanceof WorldGenRegion region)) {
            return starts;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(((LevelHolder) region).toroidal$level());
        if (transformer != null && !starts.isEmpty()) {
            List<StructureStart> framed = new ArrayList<>(starts.size());
            for (StructureStart start : starts) {
                StructureStart inFrame = toroidal$inFrameOf(region, transformer, pos, start);
                if (inFrame != null) {
                    framed.add(inFrame);
                }
            }

            starts = framed;
        }

        return starts;
    }

    // The start as the asking chunk should see it: itself when the two share a frame, its moved view otherwise — the
    // same fold ChunkGeneratorStructurePlacementMixin applies at the placement fetch.
    @Unique
    private static @Nullable StructureStart toroidal$inFrameOf(
            WorldGenRegion region, WorldLoopTransformer transformer, ChunkPos centerPos, StructureStart start) {
        if (!start.isValid()) {
            return start;
        }

        ChunkPos startPos = start.getChunkPos();
        return ((FramedStructureStart) (Object) start).toroidal$framedBy(
                region,
                transformer.chunks.x.unwrap(centerPos.x(), startPos.x()) - startPos.x(),
                transformer.chunks.z.unwrap(centerPos.z(), startPos.z()) - startPos.z());
    }

    @WrapOperation(
            method = "fillStartsForStructure",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private ChunkAccess toroidal$startChunkNearestTheGeneratingChunk(
            LevelAccessor level, int chunkX, int chunkZ, ChunkStatus status, Operation<ChunkAccess> original) {
        if (!(level instanceof WorldGenRegion region)) {
            return original.call(level, chunkX, chunkZ, status);
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(((LevelHolder) region).toroidal$level());
        if (transformer == null) {
            return original.call(level, chunkX, chunkZ, status);
        }

        ChunkPos center = region.getCenter();
        return original.call(
                level,
                transformer.chunks.x.unwrap(center.x(), chunkX),
                transformer.chunks.z.unwrap(center.z(), chunkZ),
                status);
    }
}
