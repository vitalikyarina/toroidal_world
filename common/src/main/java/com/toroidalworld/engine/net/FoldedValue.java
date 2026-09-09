package com.toroidalworld.engine.net;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.toroidalworld.engine.fold.FoldedCopies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class FoldedValue {
    public static Object toward(TranslationContext context, Supplier<Vec3> anchor, Object value) {
        return toward(context, anchor, value, UnaryOperator.identity());
    }

    public static Object toward(TranslationContext context, Supplier<Vec3> anchor, Object value,
            UnaryOperator<Object> fallback) {
        return switch (value) {
            case BlockPos pos -> nearestCopy(context, anchor.get(), pos);
            case Vec3 position -> context.transformer().nearestCopy(anchor.get(), position);
            case ChunkPos chunkPos -> nearestCopy(context, anchor.get(), chunkPos);
            case SectionPos sectionPos -> nearestCopy(context, anchor.get(), sectionPos);
            case GlobalPos globalPos -> nearestCopy(context, anchor, globalPos);
            case Optional<?> held -> inside(context, anchor, held, fallback);
            case List<?> values -> inside(context, anchor, values, fallback);
            default -> fallback.apply(value);
        };
    }

    static BlockPos nearestCopy(TranslationContext context, Vec3 anchor, BlockPos pos) {
        return context.transformer().nearestCopy(BlockPos.containing(anchor), pos);
    }

    private static ChunkPos nearestCopy(TranslationContext context, Vec3 anchor, ChunkPos chunkPos) {
        return context.transformer().nearestCopy(new ChunkPos(BlockPos.containing(anchor)), chunkPos);
    }

    private static SectionPos nearestCopy(TranslationContext context, Vec3 anchor, SectionPos sectionPos) {
        ChunkPos chunkPos = sectionPos.chunk();
        ChunkPos clientChunkPos = nearestCopy(context, anchor, chunkPos);
        return clientChunkPos == chunkPos ? sectionPos : SectionPos.of(clientChunkPos, sectionPos.y());
    }

    private static GlobalPos nearestCopy(TranslationContext context, Supplier<Vec3> anchor, GlobalPos globalPos) {
        if (!globalPos.dimension().equals(context.dimension())) {
            return globalPos;
        }

        BlockPos clientPos = nearestCopy(context, anchor.get(), globalPos.pos());
        return clientPos == globalPos.pos() ? globalPos : GlobalPos.of(globalPos.dimension(), clientPos);
    }

    private static Optional<?> inside(TranslationContext context, Supplier<Vec3> anchor, Optional<?> held,
            UnaryOperator<Object> fallback) {
        Object value = held.orElse(null);
        if (value == null) {
            return held;
        }

        Object clientValue = toward(context, anchor, value, fallback);
        return clientValue == value ? held : Optional.of(clientValue);
    }

    @SuppressWarnings("unchecked")
    private static List<?> inside(TranslationContext context, Supplier<Vec3> anchor, List<?> values,
            UnaryOperator<Object> fallback) {
        return FoldedCopies.of((List<Object>) values, value -> toward(context, anchor, value, fallback));
    }

    private FoldedValue() {
    }
}
