package com.toroidalworld.mixin;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;

@Mixin(PoiManager.class)
public class PoiManagerMixin {
    @WrapMethod(method = "getInSquare")
    private Stream<PoiRecord> toroidal$squareThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Stream<PoiRecord>> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        PoiManager self = (PoiManager) (Object) this;
        int chunkRadius = Math.floorDiv(radius, CoordinateConstants.CHUNK_WIDTH) + 1;

        return toroidal$chunksAround(ChunkPos.containing(center), chunkRadius, transformer)
                .flatMap(chunkPos -> self.getInChunk(predicate, chunkPos, occupancy))
                .filter(record -> {
                    BlockPos pos = record.getPos();
                    BlockPos nearest = transformer.nearestCopy(center, pos);
                    return Math.abs(nearest.getX() - center.getX()) <= radius
                            && Math.abs(nearest.getZ() - center.getZ()) <= radius;
                });
    }

    @ModifyArg(
            method = "getInRange",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"),
            index = 0)
    private Predicate<PoiRecord> toroidal$rangeThroughSeam(
            Predicate<PoiRecord> original, @Local(argsOnly = true) BlockPos center, @Local(argsOnly = true) int radius) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original;
        }

        double radiusSqr = (double) radius * radius;
        return record -> toroidal$distSqr(transformer, center, record.getPos()) <= radiusSqr;
    }

    @ModifyArg(
            method = {
                    "findClosest(Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;",
                    "findClosest(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;"
            },
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;min(Ljava/util/Comparator;)Ljava/util/Optional;"),
            index = 0)
    private Comparator<BlockPos> toroidal$closestBlockThroughSeam(
            Comparator<BlockPos> original, @Local(argsOnly = true) BlockPos center) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original;
        }

        return Comparator.comparingDouble(pos -> toroidal$distSqr(transformer, center, pos));
    }

    @ModifyArg(
            method = "findClosestWithType",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;min(Ljava/util/Comparator;)Ljava/util/Optional;"),
            index = 0)
    private Comparator<PoiRecord> toroidal$closestRecordThroughSeam(
            Comparator<PoiRecord> original, @Local(argsOnly = true) BlockPos center) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original;
        }

        return Comparator.comparingDouble(record -> toroidal$distSqr(transformer, center, record.getPos()));
    }

    @ModifyArg(
            method = "findAllClosestFirstWithType",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"),
            index = 0)
    private Comparator<Pair<Holder<PoiType>, BlockPos>> toroidal$sortThroughSeam(
            Comparator<Pair<Holder<PoiType>, BlockPos>> original, @Local(argsOnly = true) BlockPos center) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original;
        }

        return Comparator.comparingDouble(pair -> toroidal$distSqr(transformer, center, pair.getSecond()));
    }

    @WrapOperation(
            method = "ensureLoadedAndValid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;aroundChunk(Lnet/minecraft/world/level/ChunkPos;III)Ljava/util/stream/Stream;"))
    private Stream<SectionPos> toroidal$sectionsThroughSeam(
            ChunkPos center, int chunkRadius, int minSectionY, int maxSectionY, Operation<Stream<SectionPos>> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(center, chunkRadius, minSectionY, maxSectionY);
        }

        Stream<SectionPos> wrapped = original.call(center, chunkRadius, minSectionY, maxSectionY)
                .map(section -> transformer.fold(section));

        return toroidal$foldsOntoItself(chunkRadius, transformer) ? wrapped.distinct() : wrapped;
    }

    @WrapMethod(method = "sectionsToVillage")
    private int toroidal$villageDistanceThroughSeam(SectionPos sectionPos, Operation<Integer> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(sectionPos);
        }

        return original.call(transformer.fold(sectionPos));
    }

    @ModifyVariable(
            method = {"add", "remove", "release", "exists", "getType", "getDebugPoiInfo"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private BlockPos toroidal$positionThroughSeam(BlockPos pos) {
        WorldFold transformer = toroidal$transformer();
        return transformer == null ? pos : transformer.fold(pos);
    }

    @Unique
    private static Stream<ChunkPos> toroidal$chunksAround(ChunkPos center, int chunkRadius, WorldFold transformer) {
        Stream<ChunkPos> wrapped = ChunkPos.rangeClosed(center, chunkRadius)
                .map(pos -> transformer.isOver(pos) ? transformer.fold(pos) : pos);

        if (!toroidal$foldsOntoItself(chunkRadius, transformer)) {
            return wrapped;
        }

        LongSet seen = new LongOpenHashSet();
        return wrapped.filter(chunkPos -> seen.add(chunkPos.pack()));
    }

    @Unique
    private static boolean toroidal$foldsOntoItself(int chunkRadius, WorldFold transformer) {
        int span = chunkRadius * 2 + 1;
        return transformer.bounds().x().foldsOntoItself(span) || transformer.bounds().z().foldsOntoItself(span);
    }

    @Unique
    private static double toroidal$distSqr(WorldFold transformer, BlockPos center, BlockPos pos) {
        return transformer.sqrDistance(
                center.getX(), center.getY(), center.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Unique
    private @Nullable WorldFold toroidal$levelTransformer;

    @Unique
    private @Nullable WorldFold toroidal$transformer() {
        WorldFold transformer = this.toroidal$levelTransformer;
        if (transformer == null) {
            transformer = ((SectionStorageAccessor) this).toroidal$getLevelHeightAccessor() instanceof ServerLevel level
                    ? WorldLoopAttachments.transformerOf(level)
                    : WorldFolds.NOOP;
            this.toroidal$levelTransformer = transformer;
        }

        return transformer.isWrapped() ? transformer : null;
    }
}
