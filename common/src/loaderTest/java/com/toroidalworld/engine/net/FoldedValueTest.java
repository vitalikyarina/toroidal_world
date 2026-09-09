package com.toroidalworld.engine.net;

import static com.toroidalworld.engine.net.PacketTranslatorFixture.CLIENT_BLOCK;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.CLIENT_CHUNK;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.CLIENT_X;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.CLIENT_Z;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.MIRROR_X;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.MIRROR_Z;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.SERVER_BLOCK;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.SERVER_CHUNK;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.SERVER_X;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.SERVER_Z;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.context;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class FoldedValueTest {
    private static final Supplier<Vec3> ANCHOR = () -> new Vec3(MIRROR_X, 0.0, MIRROR_Z);

    private static final int SECTION_Y = 4;

    private static final String UNFOLDED = "a label the fold knows nothing about";

    private static Object toward(Object value) {
        return FoldedValue.toward(context(), ANCHOR, value);
    }

    @Test
    void blockPositionMovesToTheNearestCopy() {
        assertEquals(CLIENT_BLOCK, toward(SERVER_BLOCK));
    }

    @Test
    void vectorMovesToTheNearestCopy() {
        assertEquals(new Vec3(CLIENT_X, 64.0, CLIENT_Z), toward(new Vec3(SERVER_X, 64.0, SERVER_Z)));
    }

    @Test
    void chunkPositionMovesToTheNearestCopy() {
        assertEquals(CLIENT_CHUNK, toward(SERVER_CHUNK));
    }

    @Test
    void sectionPositionMovesAndKeepsItsHeight() {
        assertEquals(SectionPos.of(CLIENT_CHUNK, SECTION_Y), toward(SectionPos.of(SERVER_CHUNK, SECTION_Y)));
    }

    @Test
    void globalPositionInThisDimensionMoves() {
        assertEquals(GlobalPos.of(Level.OVERWORLD, CLIENT_BLOCK),
                toward(GlobalPos.of(Level.OVERWORLD, SERVER_BLOCK)));
    }

    @Test
    void globalPositionInAnotherDimensionPassesThrough() {
        GlobalPos elsewhere = GlobalPos.of(Level.NETHER, SERVER_BLOCK);

        assertSame(elsewhere, toward(elsewhere));
    }

    @Test
    void optionalHoldsTheFoldedPosition() {
        assertEquals(Optional.of(CLIENT_BLOCK), toward(Optional.of(SERVER_BLOCK)));
    }

    @Test
    void emptyOptionalPassesThrough() {
        Optional<?> empty = Optional.empty();

        assertSame(empty, toward(empty));
    }

    @Test
    void optionalAlreadyInTheClientFramePassesThrough() {
        Optional<?> held = Optional.of(CLIENT_BLOCK);

        assertSame(held, toward(held));
    }

    @Test
    void listCopiesOnlyTheEntriesThatMove() {
        assertEquals(List.of(CLIENT_BLOCK, CLIENT_BLOCK), toward(List.of(SERVER_BLOCK, CLIENT_BLOCK)));
    }

    @Test
    void listWithNothingToFoldPassesThrough() {
        List<?> values = List.of(UNFOLDED, CLIENT_BLOCK);

        assertSame(values, toward(values));
    }

    @Test
    void valueOfAnUnknownTypePassesThrough() {
        assertSame(UNFOLDED, toward(UNFOLDED));
    }

    @Test
    void valueOfAnUnknownTypeReachesTheFallback() {
        assertEquals(CLIENT_BLOCK,
                FoldedValue.toward(context(), ANCHOR, UNFOLDED, replaceTheLabel()));
    }

    @Test
    void theFallbackReachesInsideAContainer() {
        assertEquals(Optional.of(CLIENT_BLOCK),
                FoldedValue.toward(context(), ANCHOR, Optional.of(UNFOLDED), replaceTheLabel()));
    }

    private static UnaryOperator<Object> replaceTheLabel() {
        return value -> UNFOLDED.equals(value) ? CLIENT_BLOCK : value;
    }
}
