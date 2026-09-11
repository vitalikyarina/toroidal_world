package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.SeamTransform;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;
import com.toroidalworld.core.FlatShape;

import net.minecraft.world.phys.Vec3;

class SableJoinDirectionTest {
    private static final int HALF_WIDTH_CHUNKS = 16;
    private static final int WIDTH_BLOCKS = HALF_WIDTH_CHUNKS * 2 * 16;
    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(
            -HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS, -HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS)));
    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(new WorldLoopBounds(
            new AxisBounds.Looped(-HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS), AxisBounds.Unbounded.INSTANCE)));

    private static final Vec3 NEAR_UPPER = new Vec3(250.0, 64.0, 0.0);
    private static final Vec3 NEAR_LOWER = new Vec3(-240.0, 64.0, 0.0);
    private static final DeckTransformation LAP_UP =
            new DeckTransformation(SeamTransform.translation(WIDTH_BLOCKS, 0));
    private static final DeckTransformation LAP_DOWN =
            new DeckTransformation(SeamTransform.translation(-WIDTH_BLOCKS, 0));

    @Test
    void twoAnchorsInOneCopyDecideNoLap() {
        assertEquals(DeckTransformation.IDENTITY,
                SableJoinDirection.lapOnto(TORUS, NEAR_UPPER, new Vec3(240.0, 64.0, 0.0)),
                "250 and 240 are 10 blocks apart the direct way, so no copy of the second is nearer");
    }

    @Test
    void theAnchorAcrossTheSeamTakesAWholeLap() {
        assertEquals(LAP_UP, SableJoinDirection.lapOnto(TORUS, NEAR_UPPER, NEAR_LOWER),
                "-240 reaches 250 over the seam at -240 + 512 = 272, which is 22 blocks away against 490");
    }

    @Test
    void twoGroupsOfOneMoveTheSideThatKeepsTheJoinedCentroidInside() {
        SableJoinDirection.Choice choice = SableJoinDirection.choose(TORUS, LAP_UP,
                List.of(NEAR_UPPER), List.of(NEAR_LOWER));

        assertEquals(new SableJoinDirection.Choice(false, LAP_DOWN), choice,
                "moving b lands (250 + 272) / 2 = 261 outside the 256-block bound, moving a lands "
                        + "(-262 - 240) / 2 = -251 inside it, and the size comparison would have moved b");
    }

    @Test
    void theSmallGroupMovesWhereThatIsWhatKeepsTheCentroidInside() {
        SableJoinDirection.Choice choice = SableJoinDirection.choose(TORUS, LAP_UP,
                List.of(NEAR_UPPER, new Vec3(200.0, 64.0, 0.0), new Vec3(150.0, 64.0, 0.0)),
                List.of(NEAR_LOWER));

        assertEquals(new SableJoinDirection.Choice(true, LAP_UP), choice,
                "moving b lands (250 + 200 + 150 + 272) / 4 = 218 inside the 256-block bound");
    }

    @Test
    void aCentroidOutsideOnTheOtherAxisFallsBackToTheSmallerGroup() {
        Vec3 upper = new Vec3(250.0, 64.0, 300.0);
        Vec3 lower = new Vec3(-240.0, 64.0, 300.0);

        SableJoinDirection.Choice choice = SableJoinDirection.choose(TORUS, LAP_UP,
                List.of(upper, new Vec3(200.0, 64.0, 300.0)), List.of(lower));

        assertEquals(new SableJoinDirection.Choice(true, LAP_UP), choice,
                "z stays at 300 outside the 256-block bound whichever side moves, so neither seats inside "
                        + "and the one-body group is the one that moves");
    }

    @Test
    void anAxisThatDoesNotLoopTakesNoLap() {
        assertEquals(LAP_UP,
                SableJoinDirection.lapOnto(CYLINDER, new Vec3(250.0, 64.0, 900.0), new Vec3(-240.0, 64.0, -900.0)),
                "x loops over 512 blocks, z is unbounded on a cylinder and reaches no copy");
    }
}
