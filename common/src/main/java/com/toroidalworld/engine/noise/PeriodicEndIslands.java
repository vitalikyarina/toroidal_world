package com.toroidalworld.engine.noise;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class PeriodicEndIslands {
    private static final int SECTION_WIDTH = 8;
    private static final int SECTIONS_PER_CHUNK = 2;

    private static final float ISLAND_PEAK_HEIGHT = 100.0F;
    private static final float MAIN_ISLAND_SIZE = 8.0F;
    private static final float MIN_HEIGHT_VALUE = -100.0F;
    private static final float MAX_HEIGHT_VALUE = 80.0F;

    private static final int ISLAND_SEARCH_CHUNK_REACH = 12;
    private static final long OUTER_ISLAND_MIN_CHUNK_DISTANCE_SQUARED = 4096L;
    private static final float ISLAND_NOISE_THRESHOLD = -0.9F;

    private static final float ISLAND_SIZE_X_FACTOR = 3439.0F;
    private static final float ISLAND_SIZE_Z_FACTOR = 147.0F;
    private static final float ISLAND_SIZE_SPREAD = 13.0F;
    private static final float ISLAND_SIZE_MIN = 9.0F;

    private static final double ZERO_DENSITY_HEIGHT_VALUE = 8.0;
    private static final double HEIGHT_VALUE_PER_DENSITY = 128.0;

    public static float heightValue(SimplexNoise islandNoise, WorldFold transformer, int blockX, int blockZ) {
        int sectionX = transformer.blockDomain(Direction.Axis.X).wrap(blockX) / SECTION_WIDTH;
        int sectionZ = transformer.blockDomain(Direction.Axis.Z).wrap(blockZ) / SECTION_WIDTH;
        float doffs = Mth.clamp(
                ISLAND_PEAK_HEIGHT - Mth.sqrt(sectionX * sectionX + sectionZ * sectionZ) * MAIN_ISLAND_SIZE,
                MIN_HEIGHT_VALUE, MAX_HEIGHT_VALUE);

        int chunkX = Math.floorDiv(blockX, CoordinateConstants.CHUNK_WIDTH);
        int chunkZ = Math.floorDiv(blockZ, CoordinateConstants.CHUNK_WIDTH);
        int subSectionX = Math.floorMod(Math.floorDiv(blockX, SECTION_WIDTH), SECTIONS_PER_CHUNK);
        int subSectionZ = Math.floorMod(Math.floorDiv(blockZ, SECTION_WIDTH), SECTIONS_PER_CHUNK);
        WrapDomain xDomain = transformer.chunkDomain(Direction.Axis.X);
        WrapDomain zDomain = transformer.chunkDomain(Direction.Axis.Z);

        for (int xo = -ISLAND_SEARCH_CHUNK_REACH; xo <= ISLAND_SEARCH_CHUNK_REACH; xo++) {
            for (int zo = -ISLAND_SEARCH_CHUNK_REACH; zo <= ISLAND_SEARCH_CHUNK_REACH; zo++) {
                long cellX = xDomain.wrap(chunkX + xo);
                long cellZ = zDomain.wrap(chunkZ + zo);
                if (cellX * cellX + cellZ * cellZ > OUTER_ISLAND_MIN_CHUNK_DISTANCE_SQUARED
                        && islandNoise.getValue(cellX, cellZ) < ISLAND_NOISE_THRESHOLD) {
                    float islandSize = (Mth.abs((float) cellX) * ISLAND_SIZE_X_FACTOR
                            + Mth.abs((float) cellZ) * ISLAND_SIZE_Z_FACTOR) % ISLAND_SIZE_SPREAD + ISLAND_SIZE_MIN;
                    float xd = subSectionX - xo * SECTIONS_PER_CHUNK;
                    float zd = subSectionZ - zo * SECTIONS_PER_CHUNK;
                    float newDoffs = Mth.clamp(ISLAND_PEAK_HEIGHT - Mth.sqrt(xd * xd + zd * zd) * islandSize,
                            MIN_HEIGHT_VALUE, MAX_HEIGHT_VALUE);
                    doffs = Math.max(doffs, newDoffs);
                }
            }
        }

        return doffs;
    }

    public static double density(float heightValue) {
        return (heightValue - ZERO_DENSITY_HEIGHT_VALUE) / HEIGHT_VALUE_PER_DENSITY;
    }

    private PeriodicEndIslands() {
    }
}
