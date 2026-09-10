package com.toroidalworld.api.v1.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.gen.FloatingCrumbs;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.toroidalworld.engine.noise.TerrainCeiling;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * What this mod does to terrain, offered to a harness that reproduces generation outside the chunk map — over many
 * seeds, in one server session, with no world on disk. Three pieces make a folded chunk what a player would see: the
 * router a seed is given, the fold bound on the thread that samples it, and the crumb sweep the carvers tail applies.
 *
 * <p>Nothing here generates a chunk. The caller drives vanilla's own generator — {@code fillFromNoise},
 * {@code buildSurface}, {@code applyCarvers} — and uses these three to make that run the folded world's rather than
 * an unbounded vanilla one.</p>
 */
public final class GenerationProbe {

    private static final int WINDOW_SNAPSHOTS = 9;

    /**
     * The noise router a folding level would build for {@code seed}, terrain ceiling and fold included, as the chunk
     * map builds the level's own. Registered {@code RandomState} hooks run for it exactly as they do for the level's.
     *
     * <p>The level's own seed is not consulted: this is the router that level's shape and options would give any
     * seed, which is what lets one session measure a thousand of them.</p>
     *
     * @throws IllegalArgumentException if the level's generator is not noise based, so there is no router to build
     */
    public static RandomState randomState(ServerLevel level, long seed) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) {
            throw new IllegalArgumentException(
                    "The level's generator is not noise based: " + generator.getClass().getName());
        }

        @Nullable WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(level);
        NoiseGeneratorSettings settings = noise.generatorSettings().value();
        NoiseGeneratorSettings shaped = fold != null ? TerrainCeiling.withCeiling(settings) : settings;
        return GenerationTransformerContext.withRouterBuild(fold,
                () -> RandomState.create(shaped, level.registryAccess().lookupOrThrow(Registries.NOISE), seed));
    }

    /**
     * Runs {@code body} with the level's fold bound on the calling thread, which is what a sampler needs to read the
     * folded field. Generation driven outside the chunk map reaches no hook that would bind it, so a density read
     * without this returns the unbounded vanilla value. The binding is per thread and lasts only for the call.
     */
    public static void withFold(ServerLevel level, Runnable body) {
        GenerationTransformerContext.runWithTransformer(WorldLoopAttachments.transformerOf(level), body);
    }

    /**
     * Clears the detached crumbs of one filled chunk, as the tail of the carvers step does for a folding level: a
     * mass under the sweep's ceiling that touches no side of the chunk takes the fluid around it. A level that does
     * not sweep — an unfolded one, or one whose generator carries no terrain ceiling — is left alone, so a caller
     * measuring the twin passes the same chunk through this call and reads no change.
     */
    public static void sweepCrumbs(ServerLevel level, ChunkAccess chunk) {
        FloatingCrumbs.sweep(level, chunk);
    }

    /**
     * What {@link #sweepCrumbs} recorded about this chunk's terrain, to be held for as long as the caller needs the
     * chunk's shape rather than the chunk. A chunk that never went through that call carries none.
     */
    public static @Nullable TerrainSnapshot snapshotOf(ChunkAccess chunk) {
        return FloatingCrumbs.snapshotOf(chunk);
    }

    /**
     * The block positions the tail of the {@code LIGHT} step clears in the centre chunk of a 3 x 3 window: a mass
     * under the sweep's ceiling that stays inside the window, made of terrain no later step has written over. Only
     * the centre chunk's own blocks are named, so a caller sweeping a lap asks once per chunk and a mass straddling
     * a border loses each of its shares to the chunk that holds it. A window with a snapshot missing names nothing.
     *
     * @param window the window's nine snapshots, row major from the lowest corner, the centre at index 4
     * @throws IllegalArgumentException if the window does not hold nine snapshots
     */
    public static long[] borderCrumbs(TerrainSnapshot[] window) {
        if (window.length != WINDOW_SNAPSHOTS) {
            throw new IllegalArgumentException(
                    "A 3 x 3 window takes nine snapshots, not " + window.length);
        }

        return FloatingCrumbs.crumbPositions(window);
    }

    private GenerationProbe() {
    }
}
