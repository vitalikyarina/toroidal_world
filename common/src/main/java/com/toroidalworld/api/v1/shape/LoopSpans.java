package com.toroidalworld.api.v1.shape;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

/**
 * What a shape declares about one dimension: which horizontal axes loop and over what span of chunks. This is the
 * whole of the geometry a shape states — the fold the engine then runs is derived from it, and none of that engine is
 * reachable from here.
 *
 * <p>Spans are half-open in chunks: {@code minChunk} is the first chunk inside the world, {@code maxChunk} the first
 * one past it. An axis with no span does not loop and stretches to the vanilla world border.
 * {@link Direction.Axis#Y} never loops and is refused by every member here.</p>
 *
 * <p>Which flat surface the world ends up being is read off the spans, not declared: both axes give a torus, one
 * gives a cylinder, neither gives an ordinary world. Immutable; every wither hands back a new instance.</p>
 */
public final class LoopSpans {
    private static final String NOT_A_HORIZONTAL_AXIS = "Not a horizontal axis: ";
    private static final String DOES_NOT_LOOP = " does not loop — check loops(axis) first";

    private record Span(int minChunk, int maxChunk) {
        private Span {
            if (minChunk >= maxChunk) {
                throw new IllegalArgumentException("A looping axis needs minChunk < maxChunk, got ["
                        + minChunk + ", " + maxChunk + ")");
            }
        }

        private int chunkWidth() {
            return this.maxChunk - this.minChunk;
        }

        private static Span ofWidth(int chunkWidth) {
            int minChunk = -(chunkWidth / 2);
            return new Span(minChunk, chunkWidth + minChunk);
        }
    }

    /** Neither axis loops — an ordinary world. */
    public static final LoopSpans NONE = new LoopSpans(null, null);

    private final @Nullable Span x;
    private final @Nullable Span z;

    private LoopSpans(@Nullable Span x, @Nullable Span z) {
        this.x = x;
        this.z = z;
    }

    /** Both axes looping over {@code chunkWidth} chunks, centred on the origin. */
    public static LoopSpans ofWidth(int chunkWidth) {
        Span span = Span.ofWidth(chunkWidth);
        return new LoopSpans(span, span);
    }

    /** One axis looping over {@code chunkWidth} chunks, centred on the origin; the other stays unbounded. */
    public static LoopSpans ofWidth(Direction.Axis axis, int chunkWidth) {
        return NONE.and(axis, Span.ofWidth(chunkWidth));
    }

    /** One axis looping over the half-open chunk span {@code [minChunk, maxChunk)}; the other stays unbounded. */
    public static LoopSpans of(Direction.Axis axis, int minChunk, int maxChunk) {
        return NONE.and(axis, new Span(minChunk, maxChunk));
    }

    /** These spans with {@code axis} looping over {@code [minChunk, maxChunk)} as well. */
    public LoopSpans and(Direction.Axis axis, int minChunk, int maxChunk) {
        return and(axis, new Span(minChunk, maxChunk));
    }

    /** Whether this axis loops. {@link Direction.Axis#Y} always answers {@code false}. */
    public boolean loops(Direction.Axis axis) {
        return axis != Direction.Axis.Y && spanOf(axis) != null;
    }

    /**
     * The first chunk inside the world on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    public int minChunk(Direction.Axis axis) {
        return looping(axis).minChunk();
    }

    /**
     * The first chunk past the world on a looping axis (exclusive bound).
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    public int maxChunk(Direction.Axis axis) {
        return looping(axis).maxChunk();
    }

    /**
     * The world's width in chunks on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    public int chunkWidth(Direction.Axis axis) {
        return looping(axis).chunkWidth();
    }

    /** Whether both axes loop over the same number of chunks. */
    public boolean isSquare() {
        return this.x != null && this.z != null && this.x.chunkWidth() == this.z.chunkWidth();
    }

    /**
     * These spans with every looping axis divided by {@code scale} and re-centred — how the nether of a shaped world
     * is normally derived from its overworld, so that the vanilla portal ratio still lands inside the world.
     *
     * @throws IllegalArgumentException if {@code scale} is not positive, or leaves a looping axis narrower than a chunk
     */
    public LoopSpans scaledDown(int scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("A scale is positive, got " + scale);
        }

        return new LoopSpans(scaledDown(this.x, scale), scaledDown(this.z, scale));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LoopSpans spans
                && Objects.equals(this.x, spans.x)
                && Objects.equals(this.z, spans.z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }

    @Override
    public String toString() {
        return "LoopSpans[x=" + text(this.x) + ", z=" + text(this.z) + "]";
    }

    private LoopSpans and(Direction.Axis axis, Span span) {
        return switch (axis) {
            case X -> new LoopSpans(span, this.z);
            case Z -> new LoopSpans(this.x, span);
            case Y -> throw new IllegalArgumentException(NOT_A_HORIZONTAL_AXIS + axis);
        };
    }

    private @Nullable Span spanOf(Direction.Axis axis) {
        return switch (axis) {
            case X -> this.x;
            case Z -> this.z;
            case Y -> null;
        };
    }

    private Span looping(Direction.Axis axis) {
        if (axis == Direction.Axis.Y) {
            throw new IllegalArgumentException(NOT_A_HORIZONTAL_AXIS + axis);
        }

        Span span = spanOf(axis);
        if (span == null) {
            throw new IllegalArgumentException("Axis " + axis + DOES_NOT_LOOP);
        }

        return span;
    }

    private static @Nullable Span scaledDown(@Nullable Span span, int scale) {
        return span == null ? null : Span.ofWidth(span.chunkWidth() / scale);
    }

    private static String text(@Nullable Span span) {
        return span == null ? "unbounded" : "[" + span.minChunk() + ".." + span.maxChunk() + ")";
    }
}
