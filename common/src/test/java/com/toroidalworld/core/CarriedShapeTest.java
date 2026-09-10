package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.shape.torus.ClimateScale;
import com.toroidalworld.shape.torus.CompactBiomes;

class CarriedShapeTest {
    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(32);
    private static final WorldLoopBounds NARROWER = WorldLoopBounds.ofWidth(16);

    private static final String NO_PREFIX = "";
    private static final String STAMP_PREFIX = ToroidalWorld.MODID + ":";

    private static final List<String> PREFIXES = List.of(NO_PREFIX, STAMP_PREFIX);

    @Test
    void everyPrefixCarriesTheShapeAndTheChosenOptionsBackUnchanged() {
        CarriedShape carried = new CarriedShape(FlatShape.torus(SQUARE),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF));

        for (String prefix : PREFIXES) {
            MapCodec<CarriedShape> codec = CarriedShape.mapCodec(prefix);
            JsonElement written = codec.codec().encodeStart(JsonOps.INSTANCE, carried).getOrThrow();

            assertTrue(written.getAsJsonObject().has(prefix + CarriedShape.WRAPPING_KEY), written.toString());
            assertTrue(written.getAsJsonObject().has(prefix + CompactBiomes.KEY), written.toString());
            assertEquals(carried, codec.codec().parse(JsonOps.INSTANCE, written).getOrThrow(), written.toString());
        }
    }

    @Test
    void anOptionLeftAtItsDefaultIsNeverWrittenAndComesBackAsTheDefault() {
        CarriedShape carried = new CarriedShape(FlatShape.torus(SQUARE));

        for (String prefix : PREFIXES) {
            MapCodec<CarriedShape> codec = CarriedShape.mapCodec(prefix);
            JsonElement written = codec.codec().encodeStart(JsonOps.INSTANCE, carried).getOrThrow();

            assertFalse(written.getAsJsonObject().has(prefix + CompactBiomes.KEY), written.toString());
            assertEquals(carried, codec.codec().parse(JsonOps.INSTANCE, written).getOrThrow(), written.toString());
        }
    }

    @Test
    void theFoldIsBuiltOnceAndHandedBackAsTheSameReference() {
        CarriedShape carried = new CarriedShape(FlatShape.torus(SQUARE));

        assertSame(carried.fold(), carried.fold());
        assertTrue(carried.fold().isWrapped());
    }

    @Test
    void aDerivedShapeKeepsTheOptionsItWasCarriedWith() {
        CarriedShape carried = new CarriedShape(FlatShape.torus(SQUARE),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF));

        CarriedShape derived = carried.withShape(FlatShape.torus(NARROWER));

        assertEquals(FlatShape.torus(NARROWER), derived.shape());
        assertEquals(carried.generationOptions(), derived.generationOptions());
    }
}
