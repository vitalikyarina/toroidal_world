package com.toroidalworld.gen;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.gen.DatapackStemOverrides.Outcome;
import com.toroidalworld.gen.DatapackStemOverrides.StemOverride;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopSizes;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class WorldShapeReport {
    private static final List<Direction.Axis> HORIZONTAL =
            List.of(Direction.Axis.X, Direction.Axis.Z);

    private static final String CODEC_SOURCE = "codec";
    private static final String STAMP_SOURCE = "stamp";
    private static final String RESTORED_SOURCE = "restored";
    private static final String STORED_SOURCE = "stored";

    public record Line(boolean broken, String text) {
    }

    public static List<Line> lines(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        FlatShape overworldShape = wrappedShapeOf(overworld);
        if (overworldShape == null) {
            String note = overworld == null
                    ? null
                    : unshapedOverworldNote(overworld.dimension().identifier(),
                            overworld.getChunkSource().getGenerator());
            return note == null ? List.of() : List.of(new Line(false, note + tail()));
        }

        List<Line> lines = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            FlatShape shape = wrappedShapeOf(level);
            if (shape != null) {
                lines.add(wrappedLine(server, level, shape));
            } else {
                lines.add(new Line(false, unwrappedLine(server, level, overworldShape)));
            }
        }

        return lines;
    }

    static @Nullable String unshapedOverworldNote(Identifier dimension, ChunkGenerator generator) {
        if (ShapedDimensions.canTakeShape(generator)) {
            return null;
        }

        return "World shape: " + dimension + " not wrapped"
                + " generator=" + generatorId(generator)
                + ", its world type brings a generator of its own that takes no world shape";
    }

    private static Line wrappedLine(MinecraftServer server, ServerLevel level, FlatShape shape) {
        WorldLoopBounds bounds = shape.bounds();
        StemOverride override = overrideOf(level);
        String violations = netherScale(server, level, bounds) + endWidth(level, bounds);
        return new Line(!violations.isEmpty(), "World shape: " + level.dimension().identifier()
                + " generator=" + generatorId(level.getChunkSource().getGenerator())
                + " shape=" + shapeSource(level, override)
                + " identification=" + shape.identification()
                + " x=" + axisSpan(bounds.x()) + " z=" + axisSpan(bounds.z()) + " chunks"
                + ", " + widths(bounds)
                + violations
                + datapackOverride(override)
                + tail());
    }

    private static String unwrappedLine(MinecraftServer server, ServerLevel level, FlatShape overworldShape) {
        double overworldScale = server.overworld().dimensionType().coordinateScale();
        double scale = level.dimensionType().coordinateScale();
        FlatShape derived = ShapedDimensions.derivedShape(overworldShape, overworldScale, scale);
        String reason = derived == null
                ? "coordinate scale " + scale + " derives no whole-chunk width in range from the overworld's "
                        + widths(overworldShape.bounds())
                : "a derivable " + widths(derived.bounds()) + " never reached the generator";
        return "World shape: " + level.dimension().identifier() + " not wrapped"
                + " generator=" + generatorId(level.getChunkSource().getGenerator())
                + ", " + reason
                + tail();
    }

    private static String shapeSource(ServerLevel level, @Nullable StemOverride override) {
        if (override != null) {
            return override.outcome() == Outcome.RESHAPED ? RESTORED_SOURCE : STORED_SOURCE;
        }

        return level.getChunkSource().getGenerator() instanceof ShapedChunkGenerator ? CODEC_SOURCE : STAMP_SOURCE;
    }

    private static @Nullable StemOverride overrideOf(ServerLevel level) {
        return DatapackStemOverrides.of(Registries.levelToLevelStem(level.dimension()));
    }

    private static String datapackOverride(@Nullable StemOverride override) {
        if (override == null) {
            return "";
        }

        return override.outcome() == Outcome.RESHAPED
                ? ", a datapack " + override.datapackGenerator() + " took the stored shape"
                : ", a datapack " + override.datapackGenerator() + " was refused the stored shape";
    }

    private static String tail() {
        return " | mod=" + Platforms.get().modVersion()
                + " mc=" + SharedConstants.getCurrentVersion().name()
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
        return WorldLoopSizes.describe(chunkWidth);
    }

    private static String netherScale(MinecraftServer server, ServerLevel level, WorldLoopBounds bounds) {
        if (level.dimension() != Level.NETHER) {
            return "";
        }

        FlatShape overworldShape = wrappedShapeOf(server.overworld());
        return overworldShape == null ? "" : netherScaleNote(overworldShape.bounds(), bounds);
    }

    static String netherScaleNote(WorldLoopBounds overworld, WorldLoopBounds nether) {
        for (Direction.Axis axis : HORIZONTAL) {
            if (sharedLoop(overworld, nether, axis)
                    && overworld.chunkWidth(axis) % nether.chunkWidth(axis) != 0) {
                return ", BROKEN portal scale on the " + axis.getName() + " axis: an overworld of "
                        + WorldLoopSizes.describe(overworld.chunkWidth(axis)) + " does not divide by a nether of "
                        + WorldLoopSizes.describe(nether.chunkWidth(axis))
                        + ", so portals will not line up across the seam";
            }
        }

        for (Direction.Axis axis : HORIZONTAL) {
            if (sharedLoop(overworld, nether, axis)) {
                return ", scale 1:" + overworld.chunkWidth(axis) / nether.chunkWidth(axis);
            }
        }

        return "";
    }

    private static String endWidth(ServerLevel level, WorldLoopBounds bounds) {
        return level.dimension() != Level.END ? "" : endWidthNote(bounds);
    }

    static String endWidthNote(WorldLoopBounds end) {
        for (Direction.Axis axis : HORIZONTAL) {
            if (end.loops(axis) && end.chunkWidth(axis) < WorldLoopSizes.END_MIN_CHUNK_WIDTH) {
                return ", BROKEN End width on the " + axis.getName() + " axis: "
                        + WorldLoopSizes.describe(end.chunkWidth(axis)) + " is under the "
                        + WorldLoopSizes.describe(WorldLoopSizes.END_MIN_CHUNK_WIDTH)
                        + " the outer islands need, so no end cities and no elytra";
            }
        }

        return "";
    }

    private static boolean sharedLoop(WorldLoopBounds overworld, WorldLoopBounds nether, Direction.Axis axis) {
        return overworld.loops(axis) && nether.loops(axis);
    }

    private WorldShapeReport() {
    }
}
