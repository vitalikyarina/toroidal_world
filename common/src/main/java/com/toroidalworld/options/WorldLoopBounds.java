package com.toroidalworld.options;

import java.util.Optional;

import com.toroidalworld.core.CoordinateConstants;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.border.WorldBorder;

// Where a looped dimension wraps: one span per horizontal axis, each either a real chunk span or no loop at all. This
// is the piece of the loop that persists — generators serialize it, the settings screen edits it, and the transformer
// derives all four wrap domains from it.
public record WorldLoopBounds(AxisBounds x, AxisBounds z) {

    // One axis of the loop: either the half-open chunk span [minChunk, maxChunk) coordinates fold into, or unbounded —
    // no seam, no width, nothing to fold. A sum type rather than a sentinel-radius span, so an axis that does not loop
    // has no numeric bounds for anyone to read, let alone compare a real distance against.
    public sealed interface AxisBounds {
        String MIN_CHUNK_KEY = "min_chunk";
        String MAX_CHUNK_KEY = "max_chunk";

        record Looped(int minChunk, int maxChunk) implements AxisBounds {
            public int chunkWidth() {
                return maxChunk - minChunk;
            }
        }

        record Unbounded() implements AxisBounds {
            public static final Unbounded INSTANCE = new Unbounded();
        }

        // An unbounded axis is written with both bounds absent. Reading accepts three shapes: a real span, an absent
        // pair, and the legacy sentinel span — files from before the explicit model spelled "does not loop" as a loop
        // out at ±LEGACY_DISABLED_AXIS_RADIUS chunks, and a hand-edited JSON may still say it that way.
        Codec<AxisBounds> CODEC = Codec.mapPair(
                        Codec.INT.optionalFieldOf(MIN_CHUNK_KEY),
                        Codec.INT.optionalFieldOf(MAX_CHUNK_KEY))
                .codec()
                .flatXmap(AxisBounds::read, AxisBounds::write);

        private static DataResult<AxisBounds> read(Pair<Optional<Integer>, Optional<Integer>> bounds) {
            Optional<Integer> minChunk = bounds.getFirst();
            Optional<Integer> maxChunk = bounds.getSecond();
            if (minChunk.isEmpty() && maxChunk.isEmpty()) {
                return DataResult.success(Unbounded.INSTANCE);
            }

            if (minChunk.isEmpty() || maxChunk.isEmpty()) {
                return DataResult.error(() -> "An axis needs both chunk bounds or neither, got only one of "
                        + MIN_CHUNK_KEY + "/" + MAX_CHUNK_KEY);
            }

            if (minChunk.get() == -LEGACY_DISABLED_AXIS_RADIUS || maxChunk.get() == LEGACY_DISABLED_AXIS_RADIUS) {
                return DataResult.success(Unbounded.INSTANCE);
            }

            if (minChunk.get() >= maxChunk.get()) {
                return DataResult.error(() -> "A looped axis needs " + MIN_CHUNK_KEY + " < " + MAX_CHUNK_KEY
                        + ", got [" + minChunk.get() + ", " + maxChunk.get() + ")");
            }

            return DataResult.success(new Looped(minChunk.get(), maxChunk.get()));
        }

        private static DataResult<Pair<Optional<Integer>, Optional<Integer>>> write(AxisBounds axis) {
            return DataResult.success(switch (axis) {
                case Looped looped -> Pair.of(Optional.of(looped.minChunk()), Optional.of(looped.maxChunk()));
                case Unbounded() -> Pair.of(Optional.empty(), Optional.empty());
            });
        }
    }

    // What the sentinel model wrote for a non-looping axis: a wrap radius past every chunk the vanilla world border
    // allows, plus slack. Kept verbatim as a parsing detail so a legacy file keeps its meaning — nothing writes it.
    private static final int LEGACY_DISABLED_AXIS_RADIUS =
            (int) (WorldBorder.MAX_CENTER_COORDINATE / CoordinateConstants.CHUNK_WIDTH) + 8192;

    private static final String X_KEY = "x";
    private static final String Z_KEY = "z";

    public static final Codec<WorldLoopBounds> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    AxisBounds.CODEC.fieldOf(X_KEY).forGetter(WorldLoopBounds::x),
                    AxisBounds.CODEC.fieldOf(Z_KEY).forGetter(WorldLoopBounds::z)
            ).apply(instance, instance.stable(WorldLoopBounds::new)));

    public static final WorldLoopBounds UNBOUNDED =
            new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, AxisBounds.Unbounded.INSTANCE);

    public WorldLoopBounds(int xMinChunk, int xMaxChunk, int zMinChunk, int zMaxChunk) {
        this(new AxisBounds.Looped(xMinChunk, xMaxChunk), new AxisBounds.Looped(zMinChunk, zMaxChunk));
    }

    // A world this many chunks wide, sitting on spawn as evenly as the width allows — an odd width just leaves one more
    // chunk on the far side. The bounds are half-open, so the width is exactly the difference between them.
    public static WorldLoopBounds ofWidth(int chunkWidth) {
        int min = -(chunkWidth / 2);
        int max = chunkWidth + min;
        return new WorldLoopBounds(min, max, min, max);
    }

    // The one shape the creation flow builds and the settings screen can represent: both axes looped at the same
    // positive chunk width. The restore path reads foreign save data only through this guard — a single-axis,
    // rectangular or degenerate world is real for the engine but has no representation on the screen, so it must not
    // be read through the partial chunkWidth() below.
    public boolean isSquare() {
        return x instanceof AxisBounds.Looped xLooped
                && z instanceof AxisBounds.Looped zLooped
                && xLooped.chunkWidth() == zLooped.chunkWidth()
                && xLooped.chunkWidth() > 0;
    }

    // The one width a fully looped square world is described by — the creation flow builds no other shape. Asking it
    // of bounds with an unbounded axis is a programming error, not a case to handle.
    public int chunkWidth() {
        return switch (x) {
            case AxisBounds.Looped looped -> looped.chunkWidth();
            case AxisBounds.Unbounded() -> throw new IllegalStateException("chunkWidth() of an unbounded axis");
        };
    }
}
