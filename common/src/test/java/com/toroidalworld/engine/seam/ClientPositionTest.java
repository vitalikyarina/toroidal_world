package com.toroidalworld.engine.seam;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.ForeignFrame;
import com.toroidalworld.core.ForeignFrameSource;
import com.toroidalworld.core.ForeignFrames;
import com.toroidalworld.core.ForeignSpan;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class ClientPositionTest {
    private static final int WIDTH_CHUNKS = 32;
    private static final double WIDTH_BLOCKS = 512.0;
    private static final FlatShape TORUS_SHAPE = FlatShape.torus(WorldLoopBounds.ofWidth(WIDTH_CHUNKS));
    private static final WorldFold TORUS = WorldFolds.of(TORUS_SHAPE);
    private static final WorldFold CYLINDER_X = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, WIDTH_CHUNKS)));
    private static final int PLOT_MIN_CHUNK = 1_280_000;
    private static final int PLOT_MAX_CHUNK = 1_296_384;
    private static final ForeignSpan PLOT_CHUNKS = new ForeignSpan(PLOT_MIN_CHUNK, PLOT_MAX_CHUNK);
    private static final ForeignSpan PLOT_BLOCKS = PLOT_CHUNKS.scaled(CoordinateConstants.CHUNK_WIDTH);
    private static final WorldFold FRAMED =
            WorldFolds.of(TORUS_SHAPE, List.of(new ForeignFrame(PLOT_CHUNKS, PLOT_CHUNKS)));
    private static final double PLOT_X = 20_481_032.0;
    private static final double PLOT_Z = 20_481_032.0;
    private static final double PLOT_Y = 128.0;
    private static final double SHIP_X = 300.5;
    private static final double SHIP_Z = -40.25;
    private static final double MIRROR_X = 100.5;
    private static final double MIRROR_Z = -20.25;
    private static final String HALF_WORLD_WARNING = "Half-world step invariant violated";

    static {
        ForeignFrames.register(new PlotFrameSource());
    }

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

        mirror.set(new Vec3(MIRROR_X - 2 * WIDTH_BLOCKS, PLOT_Y, MIRROR_Z + WIDTH_BLOCKS),
                MirrorWriter.PLAYER_MOVE);

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

        mirror.set(new Vec3(MIRROR_X + WIDTH_BLOCKS, PLOT_Y, MIRROR_Z), MirrorWriter.POSITION_PACKET);

        assertEquals(MIRROR_X + WIDTH_BLOCKS, mirror.x());
        assertEquals(MIRROR_Z, mirror.z());
        assertEquals(1, warnings.size(), warnings.toString());
        String warning = warnings.get(0);
        assertTrue(warning.startsWith(HALF_WORLD_WARNING), warning);
        assertTrue(warning.contains("by position_packet"), warning);
        assertTrue(warning.contains("mirror x stepped from 100.5 to 612.5"), warning);
    }

    @Test
    void aFrameLeavesTheInvariantStandingInsideTheWorld() {
        ClientPosition mirror = seeded(FRAMED);

        mirror.set(new Vec3(MIRROR_X + WIDTH_BLOCKS, PLOT_Y, MIRROR_Z), MirrorWriter.POSITION_PACKET);

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

    @Test
    void aServerAuthoredPlotCoordinateBecomesAWorldCoordinate() {
        ClientPosition mirror = seeded(FRAMED);

        mirror.set(new Vec3(PLOT_X, PLOT_Y, PLOT_Z), MirrorWriter.POSITION_PACKET);

        assertEquals(SHIP_X, mirror.x());
        assertEquals(SHIP_Z, mirror.z());
        assertEquals(List.of(), warnings);
    }

    @Test
    void theSeatReadsEveryAxisOfThePlotPosition() {
        ClientPosition mirror = seeded(FRAMED);

        mirror.set(new Vec3(PLOT_X, PLOT_Y, PLOT_Z + 10.0), MirrorWriter.POSITION_PACKET);

        assertEquals(SHIP_X - 10.0, mirror.x());
        assertEquals(SHIP_Z, mirror.z());
        assertEquals(List.of(), warnings);
    }

    @Test
    void aSeatedPlotCoordinateLandsOnTheClientsLap() {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(MIRROR_X + WIDTH_BLOCKS, MIRROR_Z, Level.OVERWORLD, null, FRAMED);

        mirror.set(new Vec3(PLOT_X, PLOT_Y, PLOT_Z), MirrorWriter.POSITION_PACKET);

        assertEquals(SHIP_X + WIDTH_BLOCKS, mirror.x());
        assertEquals(SHIP_Z, mirror.z());
        assertEquals(List.of(), warnings);
    }

    @Test
    void theMirrorFollowsTheShipAcrossSuccessivePlotWrites() {
        ClientPosition mirror = seeded(FRAMED);

        mirror.set(new Vec3(PLOT_X, PLOT_Y, PLOT_Z), MirrorWriter.POSITION_PACKET);
        mirror.set(new Vec3(PLOT_X + 5.0, PLOT_Y, PLOT_Z), MirrorWriter.POSITION_PACKET);

        assertEquals(SHIP_X, mirror.x());
        assertEquals(SHIP_Z + 5.0, mirror.z());
        assertEquals(List.of(), warnings);
    }

    @Test
    void anUnseededMirrorAcceptsAPositionPacket() {
        ClientPosition mirror = new ClientPosition();

        assertDoesNotThrow(() ->
                mirror.set(new Vec3(WIDTH_BLOCKS, PLOT_Y, WIDTH_BLOCKS), MirrorWriter.POSITION_PACKET));
        assertEquals(List.of(), warnings);
    }

    private static ClientPosition seeded(WorldFold fold) {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, null, fold);
        return mirror;
    }

    private static org.apache.logging.log4j.core.Logger logger() {
        return ((LoggerContext) LogManager.getContext(false)).getLogger(ClientPosition.class.getName());
    }

    private static final class PlotFrameSource implements ForeignFrameSource {
        @Override
        public Optional<ForeignFrame> frameOf(Level level) {
            return Optional.empty();
        }

        @Override
        public Vec3 seatInWorld(@Nullable Level level, Vec3 stored) {
            if (!PLOT_BLOCKS.contains(stored.x) || !PLOT_BLOCKS.contains(stored.z)) {
                return stored;
            }

            double dx = stored.x - PLOT_X;
            double dz = stored.z - PLOT_Z;
            return new Vec3(SHIP_X - dz, stored.y, SHIP_Z + dx);
        }
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
