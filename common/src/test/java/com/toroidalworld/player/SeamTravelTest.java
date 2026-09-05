package com.toroidalworld.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.Codec;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class SeamTravelTest {
    private static final int WIDTH_CHUNKS = 32;
    private static final double WIDTH_BLOCKS = 512.0;
    private static final double MAX_BLOCK = 255.0;
    private static final double MIN_BLOCK = -256.0;
    private static final double TOLERANCE = 1.0E-9;
    private static final WorldFold TORUS =
            WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(WIDTH_CHUNKS)));
    private static final WorldFold CYLINDER_X =
            WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, WIDTH_CHUNKS)));

    @Test
    void pacingBackAndForthNeverClosesALap() {
        SeamTravel travel = seeded(TORUS, Level.OVERWORLD, 0.0, 0.0);

        for (int leg = 0; leg < 4; leg++) {
            assertEquals(Set.of(), stepTo(travel, TORUS, Level.OVERWORLD, 200.0, 0.0).closed());
            assertEquals(Set.of(), stepTo(travel, TORUS, Level.OVERWORLD, 0.0, 0.0).closed());
        }

        assertEquals(0.0, travel.in(Level.OVERWORLD).x(), TOLERANCE);
    }

    @Test
    void aLapAcrossTheSeamClosesOnceAndKeepsTheRemainder() {
        SeamTravel travel = seeded(TORUS, Level.OVERWORLD, 0.0, 0.0);

        stepTo(travel, TORUS, Level.OVERWORLD, 200.0, 0.0);
        stepTo(travel, TORUS, Level.OVERWORLD, -112.0, 0.0);
        SeamTravel.Step closing = stepTo(travel, TORUS, Level.OVERWORLD, 30.0, 0.0);

        assertEquals(Set.of(Direction.Axis.X), closing.closed());
        assertEquals(30.0, travel.in(Level.OVERWORLD).x(), TOLERANCE);
    }

    @Test
    void aLapAgainstTheAxisClosesToo() {
        SeamTravel travel = seeded(TORUS, Level.OVERWORLD, 0.0, 0.0);

        stepTo(travel, TORUS, Level.OVERWORLD, 0.0, -200.0);
        stepTo(travel, TORUS, Level.OVERWORLD, 0.0, 112.0);
        SeamTravel.Step closing = stepTo(travel, TORUS, Level.OVERWORLD, 0.0, -30.0);

        assertEquals(Set.of(Direction.Axis.Z), closing.closed());
        assertEquals(-30.0, travel.in(Level.OVERWORLD).z(), TOLERANCE);
    }

    @Test
    void crossingTheSeamCountsTheShortStepAndNotTheWorldWidth() {
        SeamTravel travel = seeded(TORUS, Level.OVERWORLD, MAX_BLOCK, 0.0);

        SeamTravel.Step crossing = stepTo(travel, TORUS, Level.OVERWORLD, MIN_BLOCK, 0.0);

        assertEquals(-(WIDTH_BLOCKS - 1.0), crossing.raw().x, TOLERANCE);
        assertEquals(1.0, crossing.folded().x, TOLERANCE);
        assertEquals(1.0, travel.in(Level.OVERWORLD).x(), TOLERANCE);
    }

    @Test
    void anUnboundedAxisNeverAccumulatesAndNeverCloses() {
        SeamTravel travel = seeded(CYLINDER_X, Level.OVERWORLD, 0.0, 0.0);

        SeamTravel.Step step = stepTo(travel, CYLINDER_X, Level.OVERWORLD, 0.0, 4.0 * WIDTH_BLOCKS);

        assertEquals(Set.of(), step.closed());
        assertEquals(0.0, travel.in(Level.OVERWORLD).z(), TOLERANCE);
    }

    @Test
    void aDimensionChangeIsNotTravel() {
        SeamTravel travel = seeded(TORUS, Level.OVERWORLD, 0.0, 0.0);
        stepTo(travel, TORUS, Level.OVERWORLD, 200.0, 0.0);

        SeamTravel.Step arrival = stepTo(travel, TORUS, Level.NETHER, -200.0, 0.0);

        assertTrue(!arrival.moved());
        assertEquals(0.0, travel.in(Level.NETHER).x(), TOLERANCE);
        assertEquals(200.0, travel.in(Level.OVERWORLD).x(), TOLERANCE);
    }

    @Test
    void eachDimensionKeepsItsOwnTotalAcrossAReturnTrip() {
        SeamTravel travel = seeded(TORUS, Level.OVERWORLD, 0.0, 0.0);
        stepTo(travel, TORUS, Level.OVERWORLD, 200.0, 0.0);

        stepTo(travel, TORUS, Level.NETHER, 0.0, 0.0);
        stepTo(travel, TORUS, Level.NETHER, 50.0, 0.0);

        stepTo(travel, TORUS, Level.OVERWORLD, 200.0, 0.0);
        stepTo(travel, TORUS, Level.OVERWORLD, 240.0, 0.0);

        assertEquals(240.0, travel.in(Level.OVERWORLD).x(), TOLERANCE);
        assertEquals(50.0, travel.in(Level.NETHER).x(), TOLERANCE);
    }

    @Test
    void theCodecCarriesEveryDimensionsTotal() {
        SeamTravel travel = seeded(TORUS, Level.OVERWORLD, 0.0, 0.0);
        stepTo(travel, TORUS, Level.OVERWORLD, 120.0, -30.0);
        stepTo(travel, TORUS, Level.NETHER, 0.0, 0.0);
        stepTo(travel, TORUS, Level.NETHER, 40.0, 0.0);

        Codec<SeamTravel> codec = SeamTravel.CODEC;
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, travel).getOrThrow();
        SeamTravel decoded = codec.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertEquals(120.0, decoded.in(Level.OVERWORLD).x(), TOLERANCE);
        assertEquals(-30.0, decoded.in(Level.OVERWORLD).z(), TOLERANCE);
        assertEquals(40.0, decoded.in(Level.NETHER).x(), TOLERANCE);
    }

    @Test
    void copyFromReplacesEveryDimensionsTotal() {
        SeamTravel source = seeded(TORUS, Level.OVERWORLD, 0.0, 0.0);
        stepTo(source, TORUS, Level.OVERWORLD, 77.0, 0.0);

        SeamTravel target = seeded(TORUS, Level.OVERWORLD, 0.0, 0.0);
        stepTo(target, TORUS, Level.OVERWORLD, -5.0, 0.0);
        target.copyFrom(source);

        assertEquals(77.0, target.in(Level.OVERWORLD).x(), TOLERANCE);
    }

    private static SeamTravel seeded(WorldFold fold, ResourceKey<Level> space, double x, double z) {
        SeamTravel travel = new SeamTravel();
        travel.advance(fold, space, new Vec3(x, 64.0, z));
        return travel;
    }

    private static SeamTravel.Step stepTo(SeamTravel travel, WorldFold fold, ResourceKey<Level> space,
            double x, double z) {
        return travel.advance(fold, space, new Vec3(x, 64.0, z));
    }
}
