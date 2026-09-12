package com.toroidalworld.compat.sable;

import static com.toroidalworld.compat.CompatFoldFixture.DECK_TORUS;
import static com.toroidalworld.compat.CompatFoldFixture.MIRRORED;
import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.SKEWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.WorldFold;

import net.minecraft.world.phys.Vec3;

class SableRigidShiftTest {
    private static final Vec3 NEAR_THE_SEAM = new Vec3(250.0, 70.0, 10.0);
    private static final Vec3 ACROSS_THE_SEAM = new Vec3(-250.0, 70.0, 10.0);

    @Test
    void aSeamThatTranslatesHandsItsShiftOver() {
        for (WorldFold fold : List.of(PER_AXIS, DECK_TORUS, SKEWED)) {
            DeckTransformation seat = fold.nearestCopyTransformation(NEAR_THE_SEAM, ACROSS_THE_SEAM);
            assertFalse(seat.isIdentity(), "in " + fold);

            Vector3d translation = SableRigidShift.translationOf(seat);

            assertEquals(seat.apply(ACROSS_THE_SEAM), ACROSS_THE_SEAM.add(translation.x, translation.y, translation.z),
                    "in " + fold);
        }
    }

    @Test
    void theIdentityIsNoShift() {
        assertEquals(new Vector3d(), SableRigidShift.translationOf(DeckTransformation.IDENTITY));
    }

    @Test
    void aSeamThatMirrorsIsRefused() {
        DeckTransformation seat = MIRRORED.nearestCopyTransformation(NEAR_THE_SEAM, ACROSS_THE_SEAM);
        assertFalse(seat.orientation().isIdentity());

        assertThrows(IllegalStateException.class, () -> SableRigidShift.requireTranslation(seat));
        assertThrows(IllegalStateException.class, () -> SableRigidShift.translationOf(seat));
    }
}
