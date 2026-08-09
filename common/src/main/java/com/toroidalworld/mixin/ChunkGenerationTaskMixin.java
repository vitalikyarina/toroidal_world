package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import org.slf4j.Logger;

import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ChunkGenerationTask;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GeneratingChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;

// A generation task holds its neighbourhood in one square of holders, built once and read by everything that follows:
// the layer walk that drives statuses, applyStep, and the WorldGenRegion a feature writes through. Past the bounds the
// square names phantoms, so this is the single place where the neighbour across the seam can be made real for all three
// at once. Folding only in WorldGenRegion would not do: the layer walk would never drive the wrapped chunk to the status
// the step reads, and getChunkIfPresentUnchecked would hand back null — vanilla's "Requested chunk unavailable" crash.
//
// The slot is REPLACED, never added beside: one physical chunk, one live key.
//
// The initializer is wrapped rather than the acquireGeneration call inside it, because that call lives in a lambda and a
// handler scoped to create() would match nothing at all (see conventions.md).
@Mixin(ChunkGenerationTask.class)
public class ChunkGenerationTaskMixin {
    @WrapOperation(
            method = "create",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/StaticCache2D;create(IIILnet/minecraft/util/StaticCache2D$Initializer;)Lnet/minecraft/util/StaticCache2D;"))
    private static StaticCache2D<GenerationChunkHolder> toroidal$foldSeamSlots(
            int centerX,
            int centerZ,
            int range,
            StaticCache2D.Initializer<GenerationChunkHolder> initializer,
            Operation<StaticCache2D<GenerationChunkHolder>> original,
            @Local(argsOnly = true) GeneratingChunkMap chunkMap) {
        if (!(chunkMap instanceof ChunkMap map)) {
            return original.call(centerX, centerZ, range, initializer);
        }

        ServerLevel level = ((LevelHolder) (Object) map).toroidal$level();
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(centerX, centerZ, range, initializer);
        }

        String levelName = level.dimension().location().toString();
        StaticCache2D.Initializer<GenerationChunkHolder> folding = (slotX, slotZ) -> toroidal$slotFor(
                map, transformer, levelName, initializer, centerX, centerZ, slotX, slotZ);
        return original.call(centerX, centerZ, range, folding);
    }

    @Unique
    private static final Logger toroidal$LOGGER = LogUtils.getLogger();

    @Unique
    private static GenerationChunkHolder toroidal$slotFor(
            ChunkMap map,
            WorldLoopTransformer transformer,
            String levelName,
            StaticCache2D.Initializer<GenerationChunkHolder> initializer,
            int centerX,
            int centerZ,
            int slotX,
            int slotZ) {
        // The task's own centre is never folded, whichever side of the bounds it is on. The task exists to generate that
        // chunk; handing it a different one would make it advance the wrong holder's status.
        if (slotX == centerX && slotZ == centerZ) {
            return initializer.get(slotX, slotZ);
        }

        if (!transformer.chunks.x.isOver(slotX) && !transformer.chunks.z.isOver(slotZ)) {
            return initializer.get(slotX, slotZ);
        }

        int wrappedX = transformer.chunks.x.wrap(slotX);
        int wrappedZ = transformer.chunks.z.wrap(slotZ);

        // Every out-of-bounds slot folds, whatever its ring — with the loading graph folded there is no phantom holder
        // left to answer for a raw key, and the BFS invariant (a neighbour's level trails its source by at most one per
        // hop) guarantees the physical chunk a live holder wherever a task exists to ask for it. A miss here is that
        // invariant broken, not a state to fall back from: acquireGeneration will NPE on the raw slot right after this
        // line, and the WARN is what turns that crash into a diagnosis.
        if (map.getUpdatingChunkIfPresent(ChunkPos.asLong(wrappedX, wrappedZ)) == null) {
            toroidal$LOGGER.warn(
                    "missing_folded_holder level={} slot_x={} slot_z={} wrapped_x={} wrapped_z={} center_x={} center_z={}",
                    levelName, slotX, slotZ, wrappedX, wrappedZ, centerX, centerZ);
            return initializer.get(slotX, slotZ);
        }

        return initializer.get(wrappedX, wrappedZ);
    }
}
