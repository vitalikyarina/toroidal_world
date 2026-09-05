package com.toroidalworld.compat.create;

import static com.toroidalworld.compat.CompatFoldFixture.DECK_TORUS;
import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;

class ServerFoldMemoTest {
    private static final WorldFold OVERWORLD_FOLD = PER_AXIS;
    private static final WorldFold NETHER_FOLD = DECK_TORUS;

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
