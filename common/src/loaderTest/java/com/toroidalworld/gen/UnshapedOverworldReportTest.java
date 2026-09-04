package com.toroidalworld.gen;

import static com.toroidalworld.gen.BakeStampFixture.foreignGenerator;
import static com.toroidalworld.gen.BakeStampFixture.noiseGenerator;
import static com.toroidalworld.gen.BakeStampFixture.noiseSubclassGenerator;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.dimension.LevelStem;

@Timeout(60)
class UnshapedOverworldReportTest {
    private static final Identifier OVERWORLD = LevelStem.OVERWORLD.identifier();

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void anOverworldThatTakesNoShapeIsNamedWithItsGenerator() {
        String note = WorldShapeReport.unshapedOverworldNote(OVERWORLD, foreignGenerator());

        assertNotNull(note, "a world nothing can shape produced no report line");
        assertTrue(note.contains("ForeignChunkGenerator"), note);
        assertTrue(note.contains(OVERWORLD.toString()), note);
    }

    @Test
    void anOverworldThatCouldTakeAShapeStaysSilent() {
        assertNull(WorldShapeReport.unshapedOverworldNote(OVERWORLD, noiseGenerator(worldgen)));
        assertNull(WorldShapeReport.unshapedOverworldNote(OVERWORLD, noiseSubclassGenerator(worldgen)));
    }
}
