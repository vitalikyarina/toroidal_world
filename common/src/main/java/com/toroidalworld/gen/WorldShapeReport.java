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
    private static final String CODEC_SOURCE = "codec";
    private static final String STAMP_SOURCE = "stamp";

    public static List<String> lines(MinecraftServer server) {
        FlatShape overworldShape = wrappedShapeOf(server.overworld());
        List<String> lines = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            FlatShape shape = wrappedShapeOf(level);
            if (shape != null) {
                lines.add(wrappedLine(server, level, shape));
            } else if (overworldShape != null) {
                lines.add(unwrappedLine(server, level, overworldShape));
            }
        }

        return lines;
    }

    private static String wrappedLine(MinecraftServer server, ServerLevel level, FlatShape shape) {
        WorldLoopBounds bounds = shape.bounds();
        return "World shape: " + level.dimension().location()
                + " generator=" + generatorId(level.getChunkSource().getGenerator())
                + " shape=" + shapeSource(level)
                + " identification=" + shape.identification()
                + " x=" + axisSpan(bounds.x()) + " z=" + axisSpan(bounds.z()) + " chunks"
                + ", " + widths(bounds)
                + netherScale(server, level, bounds)
                + tail();
    }

    private static String unwrappedLine(MinecraftServer server, ServerLevel level, FlatShape overworldShape) {
        double overworldScale = server.overworld().dimensionType().coordinateScale();
        double scale = level.dimensionType().coordinateScale();
        FlatShape derived = ShapedDimensions.derivedShape(overworldShape, overworldScale, scale);
        String reason = derived == null
                ? "coordinate scale " + scale + " derives no whole-chunk width in range from the overworld's "
                        + widths(overworldShape.bounds())
                : "a derivable " + widths(derived.bounds()) + " never reached the generator";
        return "World shape: " + level.dimension().location() + " not wrapped"
                + " generator=" + generatorId(level.getChunkSource().getGenerator())
                + ", " + reason
                + tail();
    }

    private static String shapeSource(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof ShapedChunkGenerator ? CODEC_SOURCE : STAMP_SOURCE;
    }

    private static String tail() {
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
