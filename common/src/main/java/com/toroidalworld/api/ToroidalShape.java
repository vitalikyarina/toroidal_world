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

    /** One block-unit coordinate folded into the world. Identity on a non-looping axis. */
    double foldCoord(Direction.Axis axis, double coord);

    /** One block-unit coordinate folded into the world. Identity on a non-looping axis. */
    int foldBlock(Direction.Axis axis, int coord);

    /** One chunk-unit coordinate folded into the world. Identity on a non-looping axis. */
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

    /** {@link #nearestCopy(Vec3, Vec3)} for one block-unit coordinate. Identity on a non-looping axis. */
    double nearestCoord(Direction.Axis axis, double ref, double coord);

    /** {@link #nearestCopy(Vec3, Vec3)} on the block grid. */
    BlockPos nearestCopy(BlockPos ref, BlockPos target);

    /**
     * The shortest vector from {@code from} to {@code to}, measured through the seam where that is shorter — what a
     * waypoint arrow, a distance readout or a direction indicator needs. Equal to
     * {@code nearestCopy(from, to).subtract(from)}.
     */
    Vec3 shortestDelta(Vec3 from, Vec3 to);
}
