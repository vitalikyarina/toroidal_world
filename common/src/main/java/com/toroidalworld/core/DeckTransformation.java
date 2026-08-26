package com.toroidalworld.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record DeckTransformation(SeamTransform blocks) {
    public static final DeckTransformation IDENTITY = new DeckTransformation(SeamTransform.IDENTITY);

    public boolean isIdentity() {
        return this.blocks.isIdentity();
    }

    public FoldOrientation orientation() {
        return this.blocks.orientation();
    }

    public BlockPos apply(BlockPos pos) {
        if (isIdentity()) {
            return pos;
        }

        return new BlockPos(this.blocks.applyCellX(pos.getX()), pos.getY(), this.blocks.applyCellZ(pos.getZ()));
    }

    public ChunkPos apply(ChunkPos chunk) {
        if (isIdentity()) {
            return chunk;
        }

        return new ChunkPos(
                SectionPos.blockToSectionCoord(this.blocks.applyCellX(chunk.getMinBlockX())),
                SectionPos.blockToSectionCoord(this.blocks.applyCellZ(chunk.getMinBlockZ())));
    }

    public BoundingBox apply(BoundingBox box) {
        if (isIdentity()) {
            return box;
        }

        int firstX = this.blocks.applyCellX(box.minX());
        int secondX = this.blocks.applyCellX(box.maxX());
        int firstZ = this.blocks.applyCellZ(box.minZ());
        int secondZ = this.blocks.applyCellZ(box.maxZ());
        return new BoundingBox(
                Math.min(firstX, secondX), box.minY(), Math.min(firstZ, secondZ),
                Math.max(firstX, secondX), box.maxY(), Math.max(firstZ, secondZ));
    }
}
