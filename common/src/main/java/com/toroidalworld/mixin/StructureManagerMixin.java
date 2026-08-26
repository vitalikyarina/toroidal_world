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
import com.toroidalworld.core.WorldFold;
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

@Mixin(StructureManager.class)
public class StructureManagerMixin {
    @Shadow
    @Final
    private LevelAccessor level;

    @WrapMethod(
            method = "startsForStructure(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Predicate;)Ljava/util/List;")
    private List<StructureStart> toroidal$startsInTheAskingChunksFrame(ChunkPos pos, Predicate<Structure> matcher,
            Operation<List<StructureStart>> original) {
        List<StructureStart> starts = original.call(pos, matcher);
        if (!(this.level instanceof WorldGenRegion region)) {
            return starts;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(((LevelHolder) region).toroidal$level());
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

    @Unique
    private static @Nullable StructureStart toroidal$inFrameOf(
            WorldGenRegion region, WorldFold transformer, ChunkPos centerPos, StructureStart start) {
        if (!start.isValid()) {
            return start;
        }

        ChunkPos startPos = start.getChunkPos();
        ChunkPos nearest = transformer.nearestCopy(centerPos, startPos);
        return ((FramedStructureStart) (Object) start).toroidal$framedBy(
                region, nearest.x() - startPos.x(), nearest.z() - startPos.z());
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

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(((LevelHolder) region).toroidal$level());
        if (transformer == null) {
            return original.call(level, chunkX, chunkZ, status);
        }

        ChunkPos nearest = transformer.nearestCopy(region.getCenter(), new ChunkPos(chunkX, chunkZ));
        return original.call(level, nearest.x(), nearest.z(), status);
    }
}
