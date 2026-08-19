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
import com.toroidalworld.core.WorldLoopTransformer;
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

// The POI search is a primitive, not a portal detail: portals, lightning rods, raids, villagers looking for a bed and
// /locate all reach the world through it. Unwrapped it walks raw chunk coordinates, so past the bounds it visits chunks
// that do not exist instead of the real ones on the other side — a bed ten blocks away across the seam is simply not
// there, and the caller concludes there is none.
//
// It comes in two layers, and both measure the seam here. The square (getInSquare) decides which chunks are looked at
// and which records survive the band filter; the range and "closest" queries (getInRange and the four rankers) then
// filter and order by distance to the centre. Vanilla measures that distance absolutely, so a record across the seam is
// scored a whole world away — included by the fixed square, then dropped by the range filter or beaten in the ranking.
// Every distance here goes through the transformer instead, so the nearest *copy* wins.
@Mixin(PoiManager.class)
public class PoiManagerMixin {
    // Vanilla-body re-implementation — verified against 26.2; re-diff on a platform bump.
    @WrapMethod(method = "getInSquare")
    private Stream<PoiRecord> toroidal$squareThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Stream<PoiRecord>> original) {
        WorldLoopTransformer transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        PoiManager self = (PoiManager) (Object) this;
        int chunkRadius = Math.floorDiv(radius, CoordinateConstants.CHUNK_WIDTH) + 1;

        return toroidal$chunksAround(new ChunkPos(center), chunkRadius, transformer)
                .flatMap(chunkPos -> self.getInChunk(predicate, chunkPos, occupancy))
                .filter(record -> {
                    BlockPos pos = record.getPos();
                    return Math.abs(transformer.coords.x.deltaFromBounds(center.getX(), pos.getX())) <= radius
                            && Math.abs(transformer.coords.z.deltaFromBounds(center.getZ(), pos.getZ())) <= radius;
                });
    }

    // getInRange narrows the square to a circle. The filter compares absolute distance, which is a whole world across the
    // seam, so the cross-seam records getInSquare now returns would be dropped again — measuring through the transformer
    // keeps them. Gates findAll / find / getRandom / take / getCountInRange, none of which rank, so this is all they need.
    @ModifyArg(
            method = "getInRange",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"),
            index = 0)
    private Predicate<PoiRecord> toroidal$rangeThroughSeam(
            Predicate<PoiRecord> original, @Local(argsOnly = true) BlockPos center, @Local(argsOnly = true) int radius) {
        WorldLoopTransformer transformer = toroidal$transformer();
        if (transformer == null) {
            return original;
        }

        double radiusSqr = (double) radius * radius;
        return record -> toroidal$distSqr(transformer, center, record.getPos()) <= radiusSqr;
    }

    // The two findClosest overloads pick the winner with .min over the record positions.
    @ModifyArg(
            method = {
                    "findClosest(Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;",
                    "findClosest(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;"
            },
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;min(Ljava/util/Comparator;)Ljava/util/Optional;"),
            index = 0)
    private Comparator<BlockPos> toroidal$closestBlockThroughSeam(
            Comparator<BlockPos> original, @Local(argsOnly = true) BlockPos center) {
        WorldLoopTransformer transformer = toroidal$transformer();
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
        WorldLoopTransformer transformer = toroidal$transformer();
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
        WorldLoopTransformer transformer = toroidal$transformer();
        if (transformer == null) {
            return original;
        }

        return Comparator.comparingDouble(pair -> toroidal$distSqr(transformer, center, pair.getSecond()));
    }

    // Without this the enumeration above has nothing to read: sections beyond the bounds are never pulled in, so the
    // wrapped chunk positions resolve to empty storage. Only positions actually past the bounds are remapped — an
    // in-range section keeps its own object — and the fold-deduplicating distinct() runs only when the radius can wrap
    // onto itself, so an ordinary POI search allocates nothing extra.
    @WrapOperation(
            method = "ensureLoadedAndValid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;aroundChunk(Lnet/minecraft/world/level/ChunkPos;III)Ljava/util/stream/Stream;"))
    private Stream<SectionPos> toroidal$sectionsThroughSeam(
            ChunkPos center, int chunkRadius, int minSectionY, int maxSectionY, Operation<Stream<SectionPos>> original) {
        WorldLoopTransformer transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(center, chunkRadius, minSectionY, maxSectionY);
        }

        Stream<SectionPos> wrapped = original.call(center, chunkRadius, minSectionY, maxSectionY)
                .map(transformer.chunks::wrapSection);

        return toroidal$foldsOntoItself(chunkRadius, transformer) ? wrapped.distinct() : wrapped;
    }

    // The derived village-distance graph is folded in SectionTrackerMixin, so after it every level it holds stands on a
    // physical section — and a question asked about a raw one past the bounds finds nothing there and reads back the
    // "no village within six sections" default. That question is asked: BehaviorUtils.findSectionClosestToVillage and
    // GolemRandomStrollInVillageGoal both enumerate SectionPos.cube around a mob and put every section in it to this
    // method, so a villager or golem standing near the seam asks about sections on the far side by their raw names.
    //
    // This is the graph's single read entry — ServerLevel.sectionsToVillage, isVillage and isCloseToVillage all come
    // through here, as do the AI callers that hold the manager directly — so the key is settled once, here, rather than
    // at each of the places that build one.
    @WrapMethod(method = "sectionsToVillage")
    private int toroidal$villageDistanceThroughSeam(SectionPos sectionPos, Operation<Integer> original) {
        WorldLoopTransformer transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(sectionPos);
        }

        return original.call(transformer.chunks.wrapSection(sectionPos));
    }

    // The searches are only half of what the manager is asked. add, remove, release, exists, getType and
    // getDebugPoiInfo each name a section with SectionPos.asLong(pos) and hand the very same position on to it, and
    // vanilla never needs any of them folded: a write arrives under a setBlock this mod already wraps, a read carries a
    // position taken off a record or a mob memory. A mod addressing the manager directly carries neither, and add then
    // builds a POI section for a chunk the world does not have and files the record where no folded search will ask.
    //
    // The fold is taken on the position and not on the section key, because the record keeps the whole BlockPos it was
    // built with and that is what every search hands back to its caller. Taking it here disturbs nothing inside the
    // section either: the world is a whole number of chunks wide, so the section-relative key comes out unchanged.
    // Folding the writes alone would be worse than folding nothing at all — a record filed physically and then released
    // by the raw name it was written with finds no section there and throws.
    @ModifyVariable(
            method = {"add", "remove", "release", "exists", "getType", "getDebugPoiInfo"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private BlockPos toroidal$positionThroughSeam(BlockPos pos) {
        WorldLoopTransformer transformer = toroidal$transformer();
        return transformer == null ? pos : transformer.blocks.wrap(pos);
    }

    // Deduplication only where it can actually happen. A search square wider than the world folds onto itself and would
    // otherwise scan the same chunk several times; a villager looking two chunks ahead never repeats, and should not pay
    // for a set on every behaviour tick. An in-bounds chunk keeps its own object rather than allocating a wrapped copy.
    @Unique
    private static Stream<ChunkPos> toroidal$chunksAround(ChunkPos center, int chunkRadius, WorldLoopTransformer transformer) {
        Stream<ChunkPos> wrapped = ChunkPos.rangeClosed(center, chunkRadius)
                .map(pos -> transformer.chunks.isOver(pos) ? transformer.chunks.wrap(pos) : pos);

        if (!toroidal$foldsOntoItself(chunkRadius, transformer)) {
            return wrapped;
        }

        LongSet seen = new LongOpenHashSet();
        return wrapped.filter(chunkPos -> seen.add(chunkPos.toLong()));
    }

    // Asked of each axis on its own: the square is scanned over itself as soon as it runs through more chunks than one
    // of them holds, and an axis that does not close never puts two of them under the same coordinate, however far the
    // search reaches along it.
    @Unique
    private static boolean toroidal$foldsOntoItself(int chunkRadius, WorldLoopTransformer transformer) {
        int span = chunkRadius * 2 + 1;
        return transformer.chunks.x.foldsOntoItself(span) || transformer.chunks.z.foldsOntoItself(span);
    }

    @Unique
    private static double toroidal$distSqr(WorldLoopTransformer transformer, BlockPos center, BlockPos pos) {
        return transformer.coords.sqrDistToBounds(
                center.getX(), center.getY(), center.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    // Resolved once for the manager's lifetime and cached — the level behind it never changes. Deliberately not volatile,
    // like LightEngineMixin: transformerOf hands back the level's one attachment instance, so a race can only cost a
    // repeated lookup, never a second transformer. NOOP is the resolved-but-unwrapped sentinel; the accessor returns
    // null for that, so an ordinary dimension keeps the vanilla path byte-for-byte.
    @Unique
    private @Nullable WorldLoopTransformer toroidal$levelTransformer;

    @Unique
    private @Nullable WorldLoopTransformer toroidal$transformer() {
        WorldLoopTransformer transformer = this.toroidal$levelTransformer;
        if (transformer == null) {
            transformer = ((SectionStorageAccessor) this).toroidal$getLevelHeightAccessor() instanceof ServerLevel level
                    ? WorldLoopAttachments.transformerOf(level)
                    : WorldLoopTransformer.NOOP;
            this.toroidal$levelTransformer = transformer;
        }

        return transformer.isWrapped() ? transformer : null;
    }
}
