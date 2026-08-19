package com.toroidalworld.gen;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.platform.Platforms;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class WorldShapeReport {
    public static List<String> lines(MinecraftServer server) {
        List<String> lines = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            WorldLoopBounds bounds = wrappedBoundsOf(level);
            if (bounds == null) {
                continue;
            }

            lines.add("World shape: " + level.dimension().location()
                    + " generator=" + generatorId(level.getChunkSource().getGenerator())
                    + " x=" + axisSpan(bounds.x()) + " z=" + axisSpan(bounds.z()) + " chunks"
                    + ", " + widths(bounds)
                    + netherScale(server, level, bounds)
                    + " | mod=" + Platforms.get().modVersion()
                    + " mc=" + SharedConstants.getCurrentVersion().getName()
                    + " loader=" + Platforms.get().loaderName() + " " + Platforms.get().loaderVersion());
        }

        return lines;
    }

    private static @Nullable WorldLoopBounds wrappedBoundsOf(@Nullable ServerLevel level) {
        if (level == null) {
            return null;
        }

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (ShapedChunkGenerator.wrappedTransformerOf(generator) == null) {
            return null;
        }

        return ((ShapedChunkGenerator) generator).wrapping();
    }

    private static String generatorId(ChunkGenerator generator) {
        return switch (generator) {
            case LoopedFlatChunkGenerator flat -> ToroidalWorld.MODID + ":" + WorldLoopGenerators.TOROIDAL_FLAT_ID;
            case LoopedChunkGenerator looped -> ToroidalWorld.MODID + ":" + WorldLoopGenerators.TOROIDAL_ID;
            default -> generator.getClass().getSimpleName();
        };
    }

    private static String axisSpan(AxisBounds axis) {
        return switch (axis) {
            case AxisBounds.Looped looped -> "[" + looped.minChunk() + ".." + looped.maxChunk() + ")";
            case AxisBounds.Unbounded() -> "unbounded";
        };
    }

    private static String widths(WorldLoopBounds bounds) {
        if (bounds.isSquare()) {
            return "width " + widthToken(bounds.chunkWidth());
        }

        StringBuilder widths = new StringBuilder("width");
        if (bounds.x() instanceof AxisBounds.Looped xLooped) {
            widths.append(" x=").append(widthToken(xLooped.chunkWidth()));
        }

        if (bounds.z() instanceof AxisBounds.Looped zLooped) {
            widths.append(" z=").append(widthToken(zLooped.chunkWidth()));
        }

        return widths.toString();
    }

    private static String widthToken(int chunkWidth) {
        return chunkWidth + " chunks (" + chunkWidth * CoordinateConstants.CHUNK_WIDTH + " blocks)";
    }

    private static String netherScale(MinecraftServer server, ServerLevel level, WorldLoopBounds bounds) {
        if (level.dimension() != Level.NETHER) {
            return "";
        }

        WorldLoopBounds overworldBounds = wrappedBoundsOf(server.overworld());
        if (overworldBounds == null || !overworldBounds.isSquare() || !bounds.isSquare()) {
            return "";
        }

        int overworldWidth = overworldBounds.chunkWidth();
        int netherWidth = bounds.chunkWidth();
        if (overworldWidth % netherWidth != 0) {
            return "";
        }

        return ", scale 1:" + overworldWidth / netherWidth;
    }

    private WorldShapeReport() {
    }
}
