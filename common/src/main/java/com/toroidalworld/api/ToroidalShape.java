package com.toroidalworld.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * The toroidal geometry of one level: which horizontal axes loop, the spans they fold into, and the folding
 * operations that geometry defines. Obtain one via {@link ToroidalWorldApi#shapeOf(net.minecraft.world.level.Level)}
 * or {@link ToroidalWorldClientApi#shapeOf(net.minecraft.client.multiplayer.ClientLevel)}; a shape exists only for
 * a level with at least one looping axis.
 *
 * <p>All spans are half-open: {@code min} is the first coordinate inside the world, {@code max} the first one past
 * it, {@code width == max - min}. {@link Direction.Axis#Y} never loops. A shape is an immutable view — cheap to
 * hold, valid as long as its level.</p>
 *
 * <p>A coordinate inside a <em>foreign frame</em> — a region a mod keeps outside the world for its own coordinate
 * space, such as the plots Sable assembles its sub-levels in — is not a lap of this world: every fold and
 * nearest-copy member hands such a coordinate back untouched, and a coordinate outside every frame folds as
 * described on each member. Which frames a level carries is decided by the mods installed, not by the shape.</p>
 */
public interface ToroidalShape {

    /** Whether this axis loops. {@link Direction.Axis#Y} always answers {@code false}. */
    boolean loops(Direction.Axis axis);

    /**
     * The first chunk inside the world on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    int minChunk(Direction.Axis axis);

    /**
     * The first chunk past the world on a looping axis (exclusive bound).
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    int maxChunk(Direction.Axis axis);

    /**
     * The world's width in chunks on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    int widthChunks(Direction.Axis axis);

    /**
     * The first block coordinate inside the world on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    int minBlock(Direction.Axis axis);

    /**
     * The first block coordinate past the world on a looping axis (exclusive bound).
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    int maxBlock(Direction.Axis axis);

    /**
     * The world's width in blocks on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    int widthBlocks(Direction.Axis axis);

    /**
     * One block-unit coordinate folded into the world. Identity on a non-looping axis.
     *
     * @throws IllegalStateException if this shape does not {@linkplain #decomposesPerAxis() decompose per axis}
     *         — where the axes are coupled one of them cannot be folded without the other, and the
     *         whole-position folds are the only correct route
     */
    double foldCoord(Direction.Axis axis, double coord);

    /**
     * One block-unit coordinate folded into the world. Identity on a non-looping axis.
     *
     * @throws IllegalStateException if this shape does not {@linkplain #decomposesPerAxis() decompose per axis}
     *         — where the axes are coupled one of them cannot be folded without the other, and the
     *         whole-position folds are the only correct route
     */
    int foldBlock(Direction.Axis axis, int coord);

    /**
     * One chunk-unit coordinate folded into the world. Identity on a non-looping axis.
     *
     * @throws IllegalStateException if this shape does not {@linkplain #decomposesPerAxis() decompose per axis}
     *         — where the axes are coupled one of them cannot be folded without the other, and the
     *         whole-position folds are the only correct route
     */
    int foldChunk(Direction.Axis axis, int chunk);

    /** The position folded into the world on both horizontal axes; Y and in-bounds positions pass through as-is. */
    BlockPos fold(BlockPos pos);

    /** The position folded into the world on both horizontal axes; Y and in-bounds positions pass through as-is. */
    Vec3 fold(Vec3 pos);

    /** The chunk position folded into the world; an in-bounds position passes through as-is. */
    ChunkPos fold(ChunkPos pos);

    /**
     * The copy of {@code target} nearest {@code ref}, each looping axis folded on its own — the coordinates a
     * renderer or a distance check should use so that something just across the seam reads as beside the reference,
     * not a world away. {@code target} may lie any number of laps out; Y passes through untouched.
     */
    Vec3 nearestCopy(Vec3 ref, Vec3 target);

    /**
     * {@link #nearestCopy(Vec3, Vec3)} for one block-unit coordinate. Identity on a non-looping axis.
     *
     * @throws IllegalStateException if this shape does not {@linkplain #decomposesPerAxis() decompose per axis}
     *         — where the axes are coupled one of them cannot be folded without the other, and the
     *         whole-position folds are the only correct route
     */
    double nearestCoord(Direction.Axis axis, double ref, double coord);

    /** {@link #nearestCopy(Vec3, Vec3)} on the block grid. */
    BlockPos nearestCopy(BlockPos ref, BlockPos target);

    /**
     * The shortest vector from {@code from} to {@code to}, measured through the seam where that is shorter — what a
     * waypoint arrow, a distance readout or a direction indicator needs. Equal to
     * {@code nearestCopy(from, to).subtract(from)}.
     */
    Vec3 shortestDelta(Vec3 from, Vec3 to);

    /**
     * Whether this shape's horizontal axes fold independently of one another. {@code false} where crossing a seam
     * on one axis moves or flips the other, and there the per-axis members throw: only the whole-position folds
     * answer correctly.
     */
    boolean decomposesPerAxis();

    /**
     * Whether a fold leaves a position's index inside its own chunk alone. {@code false} on a shape whose seam
     * mirrors, where crossing it reverses the local indices, so anything keyed by the low bits of a coordinate —
     * block state arrays, heightmaps, post-processing shorts — must be rebuilt rather than carried across.
     */
    boolean preservesLocalIndices();

    /**
     * How a fold turned the space around the position it folded: which horizontal axes it reversed. Always
     * {@link Orientation#IDENTITY} on a shape that {@link ToroidalShape#preservesLocalIndices() preserves local indices};
     * {@link Direction.Axis#Y} is never reversed.
     */
    record Orientation(boolean flipsX, boolean flipsZ) {
        /** The fold reversed nothing — the only orientation an unmirrored shape ever reports. */
        public static final Orientation IDENTITY = new Orientation(false, false);

        public boolean isIdentity() {
            return !this.flipsX && !this.flipsZ;
        }

        /** Whether the fold kept handedness: a reversal on both axes is a half turn, not a mirror. */
        public boolean preservesHandedness() {
            return this.flipsX == this.flipsZ;
        }

        /**
         * A direction, velocity or offset carried across the same fold. Apply this to any vector that travelled
         * with the folded position, or it will point the wrong way on the far side of a mirrored seam.
         */
        public Vec3 applyToDelta(Vec3 delta) {
            if (isIdentity()) {
                return delta;
            }

            return new Vec3(this.flipsX ? -delta.x : delta.x, delta.y, this.flipsZ ? -delta.z : delta.z);
        }
    }

    /** A folded value together with the {@link Orientation} the fold applied to reach it. */
    record Oriented<T>(T value, Orientation orientation) {
        public boolean isIdentity() {
            return this.orientation.isIdentity();
        }
    }

    /** {@link #fold(BlockPos)}, reporting the orientation the fold applied. */
    Oriented<BlockPos> foldOriented(BlockPos pos);

    /** {@link #fold(Vec3)}, reporting the orientation the fold applied. */
    Oriented<Vec3> foldOriented(Vec3 pos);

    /** {@link #fold(ChunkPos)}, reporting the orientation the fold applied. */
    Oriented<ChunkPos> foldOriented(ChunkPos pos);

    /** {@link #nearestCopy(Vec3, Vec3)}, reporting the orientation of the copy it chose. */
    Oriented<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target);

    /** {@link #nearestCopy(BlockPos, BlockPos)}, reporting the orientation of the copy it chose. */
    Oriented<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target);
}
