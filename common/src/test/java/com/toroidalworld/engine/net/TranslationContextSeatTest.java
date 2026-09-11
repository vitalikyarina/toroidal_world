package com.toroidalworld.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.engine.seam.ClientPosition;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class TranslationContextSeatTest {
    private static final int WIDTH_IN_CHUNKS = 32;
    private static final int WIDTH = WIDTH_IN_CHUNKS * 16;
    private static final int VIEW_DISTANCE = 12;

    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(WIDTH_IN_CHUNKS)));

    private static final Vec3 TARGET = new Vec3(240.5, 64.0, 10.5);
    private static final double ANCHOR_BESIDE_THE_TARGET = 240.0;
    private static final double ANCHOR_ACROSS_THE_SEAM = -240.0;

    private static TranslationContext contextAnchoredAt(double x) {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(x, 0.0, Level.OVERWORLD, TORUS);
        return new TranslationContext(
                TORUS,
                clientPosition,
                size -> {
                    throw new UnsupportedOperationException();
                },
                Level.OVERWORLD,
                VIEW_DISTANCE,
                VIEW_DISTANCE,
                entityId -> false,
                entityId -> null,
                entityId -> null,
                () -> {
                },
                PacketTranslator.production());
    }

    @Test
    void theTransformationTowardAnAnchorBesideTheTargetIsTheIdentityItself() {
        assertSame(DeckTransformation.IDENTITY,
                contextAnchoredAt(ANCHOR_BESIDE_THE_TARGET).nearestCopyTransformation(TARGET));
    }

    @Test
    void theTransformationTowardAnAnchorAcrossTheSeamSeatsThePointInTheLappedCopy() {
        TranslationContext context = contextAnchoredAt(ANCHOR_ACROSS_THE_SEAM);
        assertEquals(TARGET.subtract(WIDTH, 0.0, 0.0), context.nearestCopyTransformation(TARGET).apply(TARGET));
    }

    @Test
    void theTransformationSeatsThePointWhereTheContextSeatsIt() {
        TranslationContext context = contextAnchoredAt(ANCHOR_ACROSS_THE_SEAM);
        assertEquals(context.nearestCopy(TARGET), context.nearestCopyTransformation(TARGET).apply(TARGET));
    }
}
