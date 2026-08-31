package com.toroidalworld.gen;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class WorldShapeReport {
    public static List<String> lines(MinecraftServer server) {
        List<String> lines = new ArrayList<>();
        boolean anyShaped = false;
        for (ServerLevel level : server.getAllLevels()) {
            FlatShape shape = wrappedShapeOf(level);
            lines.add(shape == null ? unshapedLine(level) : shapedLine(server, level, shape));
            anyShaped |= shape != null;
        }

        return anyShaped ? lines : List.of();
    }

    private static String shapedLine(MinecraftServer server, ServerLevel level, FlatShape shape) {
        WorldLoopBounds bounds = shape.bounds();
        return "World shape: " + level.dimension().location()
                + " generator=" + generatorId(level.getChunkSource().getGenerator())
                + " identification=" + shape.identification()
                + " x=" + axisSpan(bounds.x()) + " z=" + axisSpan(bounds.z()) + " chunks"
                + ", " + widths(bounds)
                + netherScale(server, level, bounds)
                + environment();
    }

    private static String unshapedLine(ServerLevel level) {
        return "World shape: " + level.dimension().location()
                + " generator=" + generatorId(level.getChunkSource().getGenerator())
                + " unshaped"
                + environment();
    }

    private static String environment() {
        return " | mod=" + Platforms.get().modVersion()
                + " mc=" + SharedConstants.getCurrentVersion().getName()
                + " loader=" + Platforms.get().loaderName() + " " + Platforms.get().loaderVersion();
    }

    private static @Nullable FlatShape wrappedShapeOf(@Nullable ServerLevel level) {
        return level == null ? null : ShapedChunkGenerator.wrappedShapeOf(level.getChunkSource().getGenerator());
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

        FlatShape overworldShape = wrappedShapeOf(server.overworld());
        if (overworldShape == null || !overworldShape.bounds().isSquare() || !bounds.isSquare()) {
            return "";
        }

        int overworldWidth = overworldShape.bounds().chunkWidth();
        int netherWidth = bounds.chunkWidth();
        if (overworldWidth % netherWidth != 0) {
            return "";
        }

        return ", scale 1:" + overworldWidth / netherWidth;
    }

    private WorldShapeReport() {
    }
}
