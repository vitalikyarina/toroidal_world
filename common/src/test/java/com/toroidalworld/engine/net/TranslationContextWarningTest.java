package com.toroidalworld.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.engine.seam.ClientPosition;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class TranslationContextWarningTest {
    private static final int WIDTH_IN_CHUNKS = 32;
    private static final int VIEW_DISTANCE = 8;
    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(WIDTH_IN_CHUNKS)));

    private static final ChunkPos CHUNK_PAST_THE_VIEW = new ChunkPos(12, 0);
    private static final double COORD_PAST_THE_REACH = 100.0;
    private static final Vec3 POSITION_PAST_THE_REACH = new Vec3(COORD_PAST_THE_REACH, 64.0, COORD_PAST_THE_REACH);

    private static final String PARTICLE_KIND = "particle";
    private static final double PARTICLE_RADIUS = 32.0;
    private static final PacketReach PARTICLE = PacketReach.measured(PARTICLE_KIND, PARTICLE_RADIUS);

    private static final double ANCHOR_NEAR_THE_SEAM = 250.0;
    private static final Vec3 POSITION_ACROSS_THE_SEAM = new Vec3(-200.0, 64.0, -200.0);
    private static final Vec3 ITS_NEAREST_COPY = new Vec3(312.0, 64.0, 312.0);

    private static final String CHUNK_WARNING = "A chunk lands farther from the client anchor";
    private static final String COORD_WARNING_X = "packet's x lands farther from the client anchor";
    private static final String COORD_WARNING_Z = "packet's z lands farther from the client anchor";

    private final List<String> warnings = new ArrayList<>();
    private final CapturingAppender appender = new CapturingAppender(warnings);

    @BeforeEach
    void captureWarnings() {
        appender.start();
        logger().addAppender(appender);
    }

    @AfterEach
    void releaseWarnings() {
        logger().removeAppender(appender);
        appender.stop();
    }

    @Test
    void aChunkAndACoordinateWarningInTheSameSecondBothReachTheLog() {
        TranslationContext context = contextAtTheOrigin();

        context.toClient(CHUNK_PAST_THE_VIEW);
        context.toClientX(COORD_PAST_THE_REACH, PARTICLE);

        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).startsWith(CHUNK_WARNING), warnings.toString());
        assertTrue(warnings.get(1).contains(COORD_WARNING_X), warnings.toString());
    }

    @Test
    void bothAxesOfOnePositionReachTheLog() {
        TranslationContext context = contextAtTheOrigin();

        context.toClient(POSITION_PAST_THE_REACH, PARTICLE);

        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains(COORD_WARNING_X), warnings.toString());
        assertTrue(warnings.get(1).contains(COORD_WARNING_Z), warnings.toString());
    }

    @Test
    void theSameWarningTwiceInOneSecondIsWrittenOnce() {
        TranslationContext context = contextAtTheOrigin();

        context.toClientX(COORD_PAST_THE_REACH, PARTICLE);
        context.toClientX(COORD_PAST_THE_REACH, PARTICLE);

        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void onePlayersWarningDoesNotConsumeAnothers() {
        TranslationContext context = contextAtTheOrigin();
        TranslationContext otherContext = contextAtTheOrigin();

        context.toClientX(COORD_PAST_THE_REACH, PARTICLE);
        otherContext.toClientX(COORD_PAST_THE_REACH, PARTICLE);

        assertEquals(2, warnings.size(), warnings.toString());
    }

    @Test
    void aSendThatMeasuredItsRadiusIsJudgedAgainstIt() {
        TranslationContext context = contextAnchoredAt(ANCHOR_NEAR_THE_SEAM);

        try (MeasuredReach ignored = MeasuredReach.measuring(PARTICLE_RADIUS)) {
            assertEquals(ITS_NEAREST_COPY, context.toClientMeasured(POSITION_ACROSS_THE_SEAM, PARTICLE_KIND));
        }

        assertEquals(2, warnings.size(), warnings.toString());
    }

    @Test
    void aSendThatMeasuredNothingIsCarriedTheSameWayAndJudgedNotAtAll() {
        TranslationContext context = contextAnchoredAt(ANCHOR_NEAR_THE_SEAM);

        assertEquals(ITS_NEAREST_COPY, context.toClientMeasured(POSITION_ACROSS_THE_SEAM, PARTICLE_KIND));

        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void theRadiusDoesNotOutliveTheSendThatMeasuredIt() {
        TranslationContext context = contextAnchoredAt(ANCHOR_NEAR_THE_SEAM);
        try (MeasuredReach ignored = MeasuredReach.measuring(PARTICLE_RADIUS)) {
            context.toClientMeasured(POSITION_ACROSS_THE_SEAM, PARTICLE_KIND);
        }

        warnings.clear();
        context.toClientMeasured(POSITION_ACROSS_THE_SEAM, PARTICLE_KIND);

        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    private static TranslationContext contextAtTheOrigin() {
        return contextAnchoredAt(0.0);
    }

    private static TranslationContext contextAnchoredAt(double anchor) {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(anchor, anchor, Level.OVERWORLD, TORUS);
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
                () -> {
                });
    }

    private static org.apache.logging.log4j.core.Logger logger() {
        return ((LoggerContext) LogManager.getContext(false)).getLogger(TranslationContext.class.getName());
    }

    private static final class CapturingAppender extends AbstractAppender {
        private final List<String> warnings;

        CapturingAppender(List<String> warnings) {
            super("translation-context-warnings", null, null, false, Property.EMPTY_ARRAY);
            this.warnings = warnings;
        }

        @Override
        public void append(LogEvent event) {
            if (event.getLoggerName().equals(TranslationContext.class.getName())) {
                warnings.add(event.getMessage().getFormattedMessage());
            }
        }
    }
}
