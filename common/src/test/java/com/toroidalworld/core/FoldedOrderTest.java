package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class FoldedOrderTest {
    private static final WorldFold SQUARE = new WorldLoopTransformer(new WorldLoopBounds(-16, 16, -16, 16));
    private static final WorldFold X_ONLY = new WorldLoopTransformer(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE));

    private static final BlockPos ANCHOR = new BlockPos(250, 64, 250);

    @Test
    void theCopyAcrossTheSeamOutranksTheOneMeasuredTheLongWayRound() {
        BlockPos anchor = new BlockPos(250, 64, 0);
        BlockPos acrossTheSeam = new BlockPos(-250, 64, 0);
        BlockPos sameSide = new BlockPos(200, 64, 0);
        Comparator<BlockPos> byRawDistance = Comparator.comparingDouble(pos -> pos.distSqr(anchor));

        assertSame(sameSide, min(byRawDistance, acrossTheSeam, sameSide));
        assertSame(acrossTheSeam, min(FoldedOrder.around(byRawDistance, SQUARE, anchor), acrossTheSeam, sameSide));
    }

    @Test
    void theOrderIsTheFoldsOwnMetricOnEveryPair() {
        for (WorldFold fold : List.of(SQUARE, X_ONLY, WorldFolds.NOOP)) {
            Comparator<BlockPos> byRawDistance = Comparator.comparingDouble(pos -> pos.distSqr(ANCHOR));
            Comparator<BlockPos> bySeamDistance = Comparator.comparingDouble(pos -> fold.sqrDistance(
                    ANCHOR.getX(), ANCHOR.getY(), ANCHOR.getZ(), pos.getX(), pos.getY(), pos.getZ()));
            Comparator<BlockPos> throughSeam = FoldedOrder.around(byRawDistance, fold, ANCHOR);

            for (BlockPos first : candidates()) {
                for (BlockPos second : candidates()) {
                    assertEquals(
                            Integer.signum(bySeamDistance.compare(first, second)),
                            Integer.signum(throughSeam.compare(first, second)),
                            fold + " ordering " + first + " against " + second);
                }
            }
        }
    }

    @Test
    void nothingToFoldReachesTheOriginalComparatorUntouched() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        BlockPos first = new BlockPos(10, 64, 10);
        BlockPos second = new BlockPos(-10, 64, -10);
        List<BlockPos> seen = new ArrayList<>();
        Comparator<BlockPos> recording = (left, right) -> {
            seen.add(left);
            seen.add(right);
            return 0;
        };

        FoldedOrder.around(recording, SQUARE, anchor).compare(first, second);

        assertEquals(List.of(first, second), seen);
        assertSame(first, seen.get(0));
        assertSame(second, seen.get(1));
    }

    @Test
    void aForeignOrderingKeepsItsRuleOnFoldedOperands() {
        BlockPos anchor = new BlockPos(250, 64, 0);
        BlockPos acrossTheSeam = new BlockPos(-250, 64, 0);
        BlockPos sameSide = new BlockPos(200, 64, 0);
        Comparator<BlockPos> byGreatestX = Comparator.<BlockPos>comparingInt(BlockPos::getX).reversed();

        assertSame(sameSide, min(byGreatestX, acrossTheSeam, sameSide));
        assertSame(acrossTheSeam, min(FoldedOrder.around(byGreatestX, SQUARE, anchor), acrossTheSeam, sameSide));
    }

    @Test
    void aPackedChunkKeyFoldsThroughTheGenericForm() {
        ChunkPos anchor = new ChunkPos(15, 0);
        Long acrossTheSeam = new ChunkPos(-16, 0).pack();
        Long sameSide = new ChunkPos(12, 0).pack();
        Comparator<Long> byRawDistance = Comparator.comparingInt(anchor::distanceSquared);
        Comparator<Long> throughSeam = FoldedOrder.of(
                byRawDistance, key -> SQUARE.nearestCopy(anchor, ChunkPos.unpack(key)).pack());

        assertEquals(sameSide, min(byRawDistance, acrossTheSeam, sameSide));
        assertEquals(acrossTheSeam, min(throughSeam, acrossTheSeam, sameSide));
    }

    private static List<BlockPos> candidates() {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = -260; x <= 260; x += 65) {
            for (int z = -260; z <= 260; z += 65) {
                positions.add(new BlockPos(x, 60, z));
            }
        }

        return positions;
    }

    private static <T> T min(Comparator<T> order, T first, T second) {
        return Stream.of(first, second).min(order).orElseThrow();
    }
}
