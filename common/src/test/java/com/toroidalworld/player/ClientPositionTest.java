package com.toroidalworld.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

import com.toroidalworld.core.ForeignFrame;
import com.toroidalworld.core.ForeignSpan;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

class ClientPositionTest {
    private static final int WIDTH_CHUNKS = 32;
    private static final double WIDTH_BLOCKS = 512.0;
    private static final FlatShape TORUS_SHAPE = FlatShape.torus(WorldLoopBounds.ofWidth(WIDTH_CHUNKS));
    private static final WorldFold TORUS = WorldFolds.of(TORUS_SHAPE);
    private static final WorldFold CYLINDER_X = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, WIDTH_CHUNKS)));
    private static final int PLOT_MIN_CHUNK = 1_280_000;
    private static final int PLOT_MAX_CHUNK = 1_296_384;
    private static final ForeignSpan PLOT_CHUNKS = new ForeignSpan(PLOT_MIN_CHUNK, PLOT_MAX_CHUNK);
    private static final WorldFold FRAMED =
            WorldFolds.of(TORUS_SHAPE, List.of(new ForeignFrame(PLOT_CHUNKS, PLOT_CHUNKS)));
    private static final double PLOT_X = 20_481_032.0;
    private static final double PLOT_Z = 20_481_032.0;
    private static final double MIRROR_X = 100.5;
    private static final double MIRROR_Z = -20.25;
    private static final String HALF_WORLD_WARNING = "Half-world step invariant violated";

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
    void aClientAuthoredWriteAWholeLapAwayStaysOnTheClientsCopy() {
        ClientPosition mirror = seeded(TORUS);

        mirror.setX(MIRROR_X + WIDTH_BLOCKS, MirrorWriter.PLAYER_MOVE);
        mirror.setZ(MIRROR_Z - WIDTH_BLOCKS, MirrorWriter.VEHICLE_MOVE);

        assertEquals(MIRROR_X, mirror.x());
        assertEquals(MIRROR_Z, mirror.z());
        assertEquals(List.of(), warnings);
    }

    @Test
    void aClientAuthoredSetSeatsBothAxes() {
        ClientPosition mirror = seeded(TORUS);

        mirror.set(MIRROR_X - 2 * WIDTH_BLOCKS, MIRROR_Z + WIDTH_BLOCKS, MirrorWriter.PLAYER_MOVE);

        assertEquals(MIRROR_X, mirror.x());
        assertEquals(MIRROR_Z, mirror.z());
        assertEquals(List.of(), warnings);
    }

    @Test
    void aClientAuthoredWriteWithinHalfAWorldIsTakenAsIs() {
        ClientPosition mirror = seeded(TORUS);

        mirror.setX(MIRROR_X + 200.0, MirrorWriter.PLAYER_MOVE);
        assertEquals(MIRROR_X + 200.0, mirror.x());

        mirror.setX(MIRROR_X + 200.0 - 255.0, MirrorWriter.PLAYER_MOVE);
        assertEquals(MIRROR_X + 200.0 - 255.0, mirror.x());
        assertEquals(List.of(), warnings);
    }

    @Test
    void aClientAuthoredWriteExactlyHalfAWorldAwayIsTakenAsIsAndDoesNotWarn() {
        ClientPosition mirror = seeded(TORUS);

        mirror.setX(MIRROR_X + WIDTH_BLOCKS / 2, MirrorWriter.PLAYER_MOVE);
        assertEquals(MIRROR_X + WIDTH_BLOCKS / 2, mirror.x());

        mirror.setZ(MIRROR_Z - WIDTH_BLOCKS / 2, MirrorWriter.VEHICLE_MOVE);
        assertEquals(MIRROR_Z - WIDTH_BLOCKS / 2, mirror.z());
        assertEquals(List.of(), warnings);
    }

    @Test
    void aServerAuthoredWriteALapAwayLandsRawAndWarns() {
        ClientPosition mirror = seeded(TORUS);

        mirror.set(MIRROR_X + WIDTH_BLOCKS, MIRROR_Z, MirrorWriter.POSITION_PACKET);

        assertEquals(MIRROR_X + WIDTH_BLOCKS, mirror.x());
        assertEquals(MIRROR_Z, mirror.z());
        assertEquals(1, warnings.size(), warnings.toString());
        String warning = warnings.get(0);
        assertTrue(warning.startsWith(HALF_WORLD_WARNING), warning);
        assertTrue(warning.contains("by position_packet"), warning);
        assertTrue(warning.contains("mirror x stepped from 100.5 to 612.5"), warning);
    }

    @Test
    void aServerAuthoredStepIntoAForeignFrameLandsRawAndDoesNotWarn() {
        ClientPosition mirror = seeded(FRAMED);

        mirror.set(PLOT_X, PLOT_Z, MirrorWriter.POSITION_PACKET);

        assertEquals(PLOT_X, mirror.x());
        assertEquals(PLOT_Z, mirror.z());
        assertEquals(List.of(), warnings);
    }

    @Test
    void aServerAuthoredStepOutOfAForeignFrameLandsRawAndDoesNotWarn() {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(PLOT_X, PLOT_Z, Level.OVERWORLD, FRAMED);

        mirror.set(MIRROR_X, MIRROR_Z, MirrorWriter.POSITION_PACKET);

        assertEquals(MIRROR_X, mirror.x());
        assertEquals(MIRROR_Z, mirror.z());
        assertEquals(List.of(), warnings);
    }

    @Test
    void aFrameLeavesTheInvariantStandingInsideTheWorld() {
        ClientPosition mirror = seeded(FRAMED);

        mirror.set(MIRROR_X + WIDTH_BLOCKS, MIRROR_Z, MirrorWriter.POSITION_PACKET);

        assertEquals(MIRROR_X + WIDTH_BLOCKS, mirror.x());
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("mirror x stepped from 100.5 to 612.5"), warnings.get(0));
    }

    @Test
    void theEndlessAxisOfACylinderTakesAnyStep() {
        ClientPosition mirror = seeded(CYLINDER_X);

        mirror.setZ(MIRROR_Z + WIDTH_BLOCKS, MirrorWriter.PLAYER_MOVE);
        assertEquals(MIRROR_Z + WIDTH_BLOCKS, mirror.z());

        mirror.setX(MIRROR_X + WIDTH_BLOCKS, MirrorWriter.PLAYER_MOVE);
        assertEquals(MIRROR_X, mirror.x());
        assertEquals(List.of(), warnings);
    }

    @Test
    void anUnseededMirrorAcceptsAClientAuthoredWrite() {
        ClientPosition mirror = new ClientPosition();

        assertDoesNotThrow(() -> mirror.setX(WIDTH_BLOCKS, MirrorWriter.PLAYER_MOVE));
        assertEquals(List.of(), warnings);
    }

    private static ClientPosition seeded(WorldFold fold) {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, fold);
        return mirror;
    }

    private static org.apache.logging.log4j.core.Logger logger() {
        return ((LoggerContext) LogManager.getContext(false)).getLogger(ClientPosition.class.getName());
    }

    private static final class CapturingAppender extends AbstractAppender {
        private final List<String> warnings;

        CapturingAppender(List<String> warnings) {
            super("client-position-warnings", null, null, false, Property.EMPTY_ARRAY);
            this.warnings = warnings;
        }

        @Override
        public void append(LogEvent event) {
            if (event.getLoggerName().equals(ClientPosition.class.getName())
                    && event.getMessage().getFormattedMessage().startsWith(HALF_WORLD_WARNING)) {
                warnings.add(event.getMessage().getFormattedMessage());
            }
        }
    }
}
