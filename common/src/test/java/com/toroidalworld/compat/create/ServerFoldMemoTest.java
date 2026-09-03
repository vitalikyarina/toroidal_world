package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

class ServerFoldMemoTest {
    private static final int WORLD_CHUNKS = 16;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static final WorldFold OVERWORLD_FOLD =
            WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold NETHER_FOLD =
            WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));

    private final Object server = new Object();
    private final Object otherServer = new Object();
    private final Object overworld = new Object();
    private final Object nether = new Object();

    @Test
    void theSameServerAndDimensionResolveOnce() {
        ServerFoldMemo memo = new ServerFoldMemo();
        AtomicInteger resolutions = new AtomicInteger();

        assertSame(OVERWORLD_FOLD, memo.of(server, overworld, counted(resolutions, OVERWORLD_FOLD)));
        assertSame(OVERWORLD_FOLD, memo.of(server, overworld, counted(resolutions, OVERWORLD_FOLD)));
        assertSame(OVERWORLD_FOLD, memo.of(server, overworld, counted(resolutions, OVERWORLD_FOLD)));
        assertEquals(1, resolutions.get());
    }

    @Test
    void anotherDimensionOnTheSameServerResolvesAgain() {
        ServerFoldMemo memo = new ServerFoldMemo();
        AtomicInteger resolutions = new AtomicInteger();

        memo.of(server, overworld, counted(resolutions, OVERWORLD_FOLD));
        assertSame(NETHER_FOLD, memo.of(server, nether, counted(resolutions, NETHER_FOLD)));
        assertEquals(2, resolutions.get());
    }

    @Test
    void theMemoNeverOutlivesTheServerItWasTakenFrom() {
        ServerFoldMemo memo = new ServerFoldMemo();
        AtomicInteger resolutions = new AtomicInteger();

        memo.of(server, overworld, counted(resolutions, OVERWORLD_FOLD));
        assertSame(NETHER_FOLD, memo.of(otherServer, overworld, counted(resolutions, NETHER_FOLD)));
        assertEquals(2, resolutions.get());
    }

    @Test
    void aDimensionWithNoFoldIsRememberedAsSuchInsteadOfResolvedEveryTime() {
        ServerFoldMemo memo = new ServerFoldMemo();
        AtomicInteger resolutions = new AtomicInteger();

        assertNull(memo.of(server, overworld, counted(resolutions, null)));
        assertNull(memo.of(server, overworld, counted(resolutions, null)));
        assertEquals(1, resolutions.get());
    }

    @Test
    void goingBackToTheDimensionBeforeLastResolvesItAgain() {
        ServerFoldMemo memo = new ServerFoldMemo();
        AtomicInteger resolutions = new AtomicInteger();

        memo.of(server, overworld, counted(resolutions, OVERWORLD_FOLD));
        memo.of(server, nether, counted(resolutions, NETHER_FOLD));
        assertSame(OVERWORLD_FOLD, memo.of(server, overworld, counted(resolutions, OVERWORLD_FOLD)));
        assertEquals(3, resolutions.get());
    }

    private static Supplier<WorldFold> counted(AtomicInteger resolutions, WorldFold fold) {
        return () -> {
            resolutions.incrementAndGet();
            return fold;
        };
    }
}
