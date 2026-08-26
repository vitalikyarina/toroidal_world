package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class FoldedCopiesTest {
    private static final BlockPos A = new BlockPos(1, 64, 1);
    private static final BlockPos B = new BlockPos(2, 64, 2);
    private static final BlockPos C = new BlockPos(3, 64, 3);
    private static final BlockPos D = new BlockPos(4, 64, 4);

    private static final UnaryOperator<BlockPos> MOVE_C = pos -> pos == C ? pos.above() : pos;

    @Test
    void aListWithNothingMovedComesBackAsTheSameObject() {
        List<BlockPos> source = List.of(A, B, C);

        assertSame(source, FoldedCopies.of(source, UnaryOperator.identity()));
    }

    @Test
    void aSetWithNothingMovedComesBackAsTheSameObject() {
        Set<BlockPos> source = Set.of(A, B, C);

        assertSame(source, FoldedCopies.of(source, UnaryOperator.identity()));
    }

    @Test
    void anEmptyCollectionComesBackAsTheSameObject() {
        List<BlockPos> source = List.of();

        assertSame(source, FoldedCopies.of(source, pos -> pos.above()));
    }

    @Test
    void theMovedElementIsReplacedAndThePrefixIsKeptAsIs() {
        List<BlockPos> folded = FoldedCopies.of(List.of(A, B, C, D), MOVE_C);

        assertEquals(List.of(A, B, C.above(), D), folded);
        assertSame(A, folded.get(0));
        assertSame(B, folded.get(1));
        assertSame(D, folded.get(3));
    }

    @Test
    void aMoveOnTheFirstElementFoldsTheWholeCollection() {
        List<BlockPos> folded = FoldedCopies.of(List.of(A, B), pos -> pos.above());

        assertEquals(List.of(A.above(), B.above()), folded);
    }

    @Test
    void aSetKeepsItsIterationOrder() {
        Set<BlockPos> source = new LinkedHashSet<>(List.of(D, C, B, A));

        Set<BlockPos> folded = FoldedCopies.of(source, MOVE_C);

        assertEquals(List.of(D, C.above(), B, A), List.copyOf(folded));
    }

    @Test
    void aBareCollectionFoldsInOrder() {
        Collection<BlockPos> source = List.of(A, C, B);

        Collection<BlockPos> folded = FoldedCopies.of(source, MOVE_C);

        assertInstanceOf(List.class, folded);
        assertEquals(List.of(A, C.above(), B), List.copyOf(folded));
    }

    @Test
    void thePrefixIsCountedByPositionNotByIdentity() {
        int[] calls = {0};
        UnaryOperator<BlockPos> moveTheThirdCall = pos -> ++calls[0] == 3 ? pos.above() : pos;

        List<BlockPos> folded = FoldedCopies.of(List.of(A, B, A, C), moveTheThirdCall);

        assertEquals(List.of(A, B, A.above(), C), folded);
        assertSame(A, folded.get(0));
        assertSame(B, folded.get(1));
    }
}
