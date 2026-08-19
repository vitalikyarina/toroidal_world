package com.toroidalworld.predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.world.phys.Vec3;

class SeamDistanceBoundsTest {
    private static final WorldLoopTransformer WORLD = new WorldLoopTransformer(new WorldLoopBounds(-16, 16, -16, 16));

    private static final Vec3 PLAYER = new Vec3(253.5, 64.0, 0.0);
    private static final Vec3 ACROSS_THE_SEAM = new Vec3(-253.5, 64.0, 0.0);

    @Test
    void horizontalAtLeastIsNoLongerSatisfiedByANeighbour() {
        DistancePredicate sniperDuel = DistancePredicate.horizontal(MinMaxBounds.Doubles.atLeast(50.0));

        assertTrue(matches(sniperDuel, PLAYER, ACROSS_THE_SEAM));
        assertFalse(matches(sniperDuel, PLAYER, nearestCopy(PLAYER, ACROSS_THE_SEAM)));
    }

    @Test
    void absoluteAtMostBecomesReachable() {
        DistancePredicate bystander = DistancePredicate.absolute(MinMaxBounds.Doubles.atMost(30.0));

        assertFalse(matches(bystander, PLAYER, ACROSS_THE_SEAM));
        assertTrue(matches(bystander, PLAYER, nearestCopy(PLAYER, ACROSS_THE_SEAM)));
    }

    @Test
    void perAxisBoundReadsTheShortSeparation() {
        DistancePredicate acrossX = new DistancePredicate(
                MinMaxBounds.Doubles.atLeast(50.0),
                MinMaxBounds.Doubles.ANY,
                MinMaxBounds.Doubles.ANY,
                MinMaxBounds.Doubles.ANY,
                MinMaxBounds.Doubles.ANY);

        assertTrue(matches(acrossX, PLAYER, ACROSS_THE_SEAM));
        assertFalse(matches(acrossX, PLAYER, nearestCopy(PLAYER, ACROSS_THE_SEAM)));
    }

    @Test
    void verticalBoundIsUntouched() {
        DistancePredicate fallFromWorldHeight = DistancePredicate.vertical(MinMaxBounds.Doubles.atLeast(379.0));
        Vec3 landing = new Vec3(-253.5, 64.0, 0.0);
        Vec3 start = new Vec3(253.5, 464.0, 0.0);
        Vec3 folded = nearestCopy(start, landing);

        assertEquals(landing.y, folded.y);
        assertTrue(matches(fallFromWorldHeight, start, landing));
        assertTrue(matches(fallFromWorldHeight, start, folded));
    }

    @Test
    void aPairThatDoesNotCrossIsLeftAlone() {
        Vec3 nearby = new Vec3(193.5, 64.0, 0.0);
        DistancePredicate sniperDuel = DistancePredicate.horizontal(MinMaxBounds.Doubles.atLeast(50.0));

        assertSame(nearby, nearestCopy(PLAYER, nearby));
        assertTrue(matches(sniperDuel, PLAYER, nearby));
    }

    private static Vec3 nearestCopy(Vec3 reference, Vec3 measured) {
        return WORLD.vectors.nearestCopy(reference, measured);
    }

    private static boolean matches(DistancePredicate bounds, Vec3 reference, Vec3 measured) {
        return bounds.matches(reference.x, reference.y, reference.z, measured.x, measured.y, measured.z);
    }
}
