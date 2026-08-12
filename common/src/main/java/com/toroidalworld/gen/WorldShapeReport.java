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

// The answer to a bug report's first question — what shape is this world — as one self-contained line per wrapped
// dimension. Each line carries the versions too: a report's log excerpt rarely arrives with the jar name attached,
// and a line that answers everything on its own survives being quoted alone. Building and formatting live here, apart
// from the emit, so a future /toroidal info command states the exact same thing without a second formatter.
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

    // Wrapped-or-not is asked exactly the way the engine itself resolves a level's transformer, so the report can
    // never disagree with the machinery it describes. Null when the dimension takes the vanilla path — those log
    // nothing, which is what makes a line's presence itself information.
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

    // The class names the id: ChunkGenerator.codec() is protected, and the two shaped generators are the mod's own,
    // so no registry lookup is needed. The class name as a fallback keeps a hypothetical third implementor honest
    // rather than mislabelled.
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

    // One width when the world is the square the creation flow builds; per-axis widths for the hand-edited shapes —
    // rectangular or single-axis — that produce the strangest reports and need the log to be exact.
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

    // The scale is not stored anywhere — vanilla's 8:1 is the portal mapping, and on a torus it must equal the ratio
    // of the two widths exactly — so it is derived here from the same bounds the line already states. Only for the
    // nether against a wrapped overworld, and only when both are square and divide evenly: in any stranger save the
    // spans above already tell the whole story, and a fabricated ratio would be a guess wearing numbers.
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
