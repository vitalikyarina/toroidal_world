package com.toroidalworld.compat.aeronautics;

import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

class SpringSeamFrameTest {
    private static final WorldFold UNWRAPPED = null;

    private static final double OWN_Y = 64.0;
    private static final double PARTNER_Y = 90.0;
    private static final double Z = 10.0;

    private static Vector3d own(double x) {
        return new Vector3d(x, OWN_Y, Z);
    }

    private static Vector3d partner(double x) {
        return new Vector3d(x, PARTNER_Y, Z);
    }

    @Test
    void aPartnerAcrossTheSeamIsSeatedOnTheSpringsSide() {
        assertEquals(partner(-250.0 + WORLD_BLOCKS), SpringSeamFrame.seat(PER_AXIS, own(250.0), partner(-250.0)));
        assertEquals(partner(250.0 - WORLD_BLOCKS), SpringSeamFrame.seat(PER_AXIS, own(-250.0), partner(250.0)));
    }

    @Test
    void aSeatedPartnerIsANewVectorAndLeavesTheArgumentWhereItWas() {
        Vector3d across = partner(-250.0);
        Vector3d seated = SpringSeamFrame.seat(PER_AXIS, own(250.0), across);

        assertNotSame(across, seated);
        assertEquals(partner(-250.0), across);
    }

    @Test
    void theSeatCarriesTheHeightItWasGiven() {
        assertEquals(PARTNER_Y, SpringSeamFrame.seat(PER_AXIS, own(250.0), partner(-250.0)).y);
    }

    @Test
    void aPartnerOnTheSpringsOwnSideComesBackByIdentity() {
        Vector3d near = partner(254.0);

        assertSame(near, SpringSeamFrame.seat(PER_AXIS, own(250.0), near));
    }

    @Test
    void anUnwrappedWorldGivesThePartnerBackByIdentity() {
        Vector3d across = partner(-250.0);

        assertSame(across, SpringSeamFrame.seat(UNWRAPPED, own(250.0), across));
        assertSame(across, SpringSeamFrame.seat(WorldFolds.NOOP, own(250.0), across));
    }
}
