package com.toroidalworld.noise;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class PeriodicEndIslands {
    public static float heightValue(SimplexNoise islandNoise, WorldLoopTransformer transformer, int blockX, int blockZ) {
        int sectionX = transformer.coords.x.wrap(blockX) / 8;
        int sectionZ = transformer.coords.z.wrap(blockZ) / 8;
        float doffs = Mth.clamp(100.0F - Mth.sqrt(sectionX * sectionX + sectionZ * sectionZ) * 8.0F, -100.0F, 80.0F);

        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        int subSectionX = Math.floorMod(Math.floorDiv(blockX, 8), 2);
        int subSectionZ = Math.floorMod(Math.floorDiv(blockZ, 8), 2);
        WrapDomain xDomain = transformer.chunks.x;
        WrapDomain zDomain = transformer.chunks.z;

        for (int xo = -12; xo <= 12; xo++) {
            for (int zo = -12; zo <= 12; zo++) {
                long cellX = xDomain.wrap(chunkX + xo);
                long cellZ = zDomain.wrap(chunkZ + zo);
                if (cellX * cellX + cellZ * cellZ > 4096L && islandNoise.getValue(cellX, cellZ) < -0.9F) {
                    float islandSize = (Mth.abs((float) cellX) * 3439.0F + Mth.abs((float) cellZ) * 147.0F) % 13.0F + 9.0F;
                    float xd = subSectionX - xo * 2;
                    float zd = subSectionZ - zo * 2;
                    float newDoffs = Mth.clamp(100.0F - Mth.sqrt(xd * xd + zd * zd) * islandSize, -100.0F, 80.0F);
                    doffs = Math.max(doffs, newDoffs);
                }
            }
        }

        return doffs;
    }

    public static double density(float heightValue) {
        return (heightValue - 8.0) / 128.0;
    }

    private PeriodicEndIslands() {
    }
}
