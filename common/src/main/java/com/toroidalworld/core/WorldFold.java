package com.toroidalworld.core;

import java.util.List;

import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface WorldFold {

    record Folded<T>(T value, FoldOrientation orientation) {
        public static <T> Folded<T> of(T value) {
            return new Folded<>(value, FoldOrientation.IDENTITY);
        }

        public boolean isIdentity() {
            return this.orientation.isIdentity();
        }
    }

    WorldLoopBounds bounds();

    boolean isWrapped();

    boolean decomposesPerAxis();

    boolean preservesLocalIndices();

    WrapDomain blockDomain(Direction.Axis axis);

    WrapDomain chunkDomain(Direction.Axis axis);

    Vec3 fold(Vec3 pos);

    BlockPos fold(BlockPos pos);

    ChunkPos fold(ChunkPos pos);

    SectionPos fold(SectionPos pos);

    long foldBlockNode(long blockNode);

    long foldChunkKey(long chunkKey);

    long foldSectionNode(long sectionNode);

    Folded<Vec3> foldOriented(Vec3 pos);

    Folded<BlockPos> foldOriented(BlockPos pos);

    Folded<ChunkPos> foldOriented(ChunkPos pos);

    Vec3 nearestCopy(Vec3 ref, Vec3 target);

    BlockPos nearestCopy(BlockPos ref, BlockPos target);

    ChunkPos nearestCopy(ChunkPos ref, ChunkPos target);

    Folded<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target);

    Folded<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target);

    Vec3 foldDelta(Vec3 from, Vec3 to);

    double sqrDistance(Vec3 from, Vec3 to);

    double sqrDistance(double xFrom, double yFrom, double zFrom, double xTo, double yTo, double zTo);

    int sqrChunkDistance(ChunkPos from, ChunkPos to);

    double sqrDistanceToBox(AABB box, Vec3 point);

    boolean crossesBounds(AABB box);

    boolean crossesBounds(BoundingBox region);

    List<Folded<AABB>> split(AABB box);

    List<Folded<BoundingBox>> split(BoundingBox region);

    boolean regionsOverlap(BoundingBox first, BoundingBox second);

    Folded<AABB> foldBox(Vec3 ref, AABB box);
}
