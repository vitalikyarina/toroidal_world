package com.toroidalworld.accessors;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

@Timeout(60)
class FramedStructureStartTest {
    private static final int WIDTH_CHUNKS = 32;
    private static final int PIECE_SPAN = 15;
    private static final int PIECE_HEIGHT = 1;

    private static final ChunkPos CENTER = new ChunkPos(0, 0);
    private static final ChunkPos INSIDE = new ChunkPos(2, 3);

    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(WIDTH_CHUNKS)));

    @Test
    void everyStructureStartCarriesTheDuck() {
        assertInstanceOf(FramedStructureStart.class, validStart(INSIDE),
                "StructureStartMixin did not apply, so the duck's default method has no receiver");
    }

    @Test
    void anInvalidStartComesBackUntouched() {
        StructureStart start = new StructureStart(null, CENTER, 0, new PiecesContainer(List.of()));

        assertSame(start, framedToward(start, CENTER),
                "an invalid start was not handed straight back");
    }

    @Test
    void aStartAlreadyNearestTheCentreComesBackItself() {
        StructureStart start = validStart(INSIDE);

        assertSame(start, framedToward(start, CENTER),
                "a start needing no deck move was copied instead of handed back");
    }

    private static StructureStart framedToward(StructureStart start, ChunkPos center) {
        WorldGenLevel unusedLevel = null;
        return ((FramedStructureStart) (Object) start).toroidal$framedToward(unusedLevel, TORUS, center);
    }

    private static StructureStart validStart(ChunkPos pos) {
        BoundingBox box = new BoundingBox(
                pos.getMinBlockX(), 0, pos.getMinBlockZ(),
                pos.getMinBlockX() + PIECE_SPAN, PIECE_HEIGHT, pos.getMinBlockZ() + PIECE_SPAN);
        return new StructureStart(null, pos, 0, new PiecesContainer(List.of(piece(box))));
    }

    private static StructurePiece piece(BoundingBox box) {
        StructurePieceType type = (context, tag) -> null;
        return new StructurePiece(type, 0, box) {
            @Override
            protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            }

            @Override
            public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                    RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            }
        };
    }
}
