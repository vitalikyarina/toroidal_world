package com.toroidalworld.probe;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

// Diagnostics for the folds re-seated onto 1.21.1, where the vanilla shape they wrapped changed rather than moved. Each
// one had to be re-anchored on a different call, and a mixin that applies proves only that the anchor exists — not that
// the fold it carries still sees the coordinate it used to see. That is what this reads.
//
// Two shapes, as the packet probe has them. A counter line per dimension, dumped on a game-time gate, answers "did this
// path run at all"; one detail line per fold per dimension, the first time that fold actually moved a coordinate,
// answers "and did it move it the right way". A fold whose calls counter climbs while its moved counter stays at zero
// is the exact shape of a re-seating that landed on the wrong anchor — the code runs, the coordinate does not change.
//
// The unwrapped counter separates the third case: the fold ran in a dimension that does not loop, where doing nothing
// is correct. Without it a wrapped world whose transformer failed to bind would read the same as a world in bounds.
public final class ReshapeProbe {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TAG = "[reshape-1211]";

    // Long enough that a dump is not itself the log's bulk, short enough to bracket a single test step.
    private static final int DUMP_INTERVAL_TICKS = 200;

    public static final String INITIAL_SPAWN = "initial_spawn";
    public static final String WORLD_SPAWN = "world_spawn";
    public static final String PLAYER_RESPAWN = "player_respawn";
    public static final String ANCHOR_SPAWN = "anchor_spawn";
    public static final String PACKET_SPAWN = "packet_spawn";
    public static final String EXPLOSION_EXPOSURE = "explosion_exposure";
    public static final String EXPLOSION_KNOCKBACK = "explosion_knockback";
    public static final String DISTANCE_BOUND = "distance_bound";
    public static final String ENTITY_PREDICATE_BOUND = "entity_predicate_bound";
    public static final String COMPASS_TARGET = "compass_target";
    public static final String EYE_SIGNAL = "eye_signal";
    public static final String EYE_STEER = "eye_steer";
    public static final String NEARBY_PLAYER = "nearby_player";
    public static final String SPAWN_CHUNK_DISTANCE = "spawn_chunk_distance";

    private static final String[] FOLDS = {
            INITIAL_SPAWN, WORLD_SPAWN, PLAYER_RESPAWN, ANCHOR_SPAWN, PACKET_SPAWN,
            EXPLOSION_EXPOSURE, EXPLOSION_KNOCKBACK, DISTANCE_BOUND, ENTITY_PREDICATE_BOUND,
            COMPASS_TARGET, EYE_SIGNAL, EYE_STEER, NEARBY_PLAYER, SPAWN_CHUNK_DISTANCE};

    private static final String CALLS = "_calls";
    private static final String MOVED = "_moved";
    private static final String UNWRAPPED = "_unwrapped";

    // A respawn point names its own dimension, and the server may no longer have that level loaded. Counted apart from
    // every dimension because there is no dimension to file it under.
    private static final AtomicLong NO_LEVEL = new AtomicLong();

    private static final Map<ResourceKey<Level>, DimensionCounters> BY_DIMENSION = new ConcurrentHashMap<>();

    private static final class DimensionCounters {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        private final Set<String> detailsLogged = ConcurrentHashMap.newKeySet();
        private final AtomicLong lastDumpedGameTime = new AtomicLong(-1L);

        // Seeded rather than created on first use, so a fold that never ran prints a zero instead of vanishing from
        // the line — "it did not happen" and "I forgot to look" have to read differently.
        private DimensionCounters() {
            for (String fold : FOLDS) {
                counters.put(fold + CALLS, new AtomicLong());
                counters.put(fold + MOVED, new AtomicLong());
                counters.put(fold + UNWRAPPED, new AtomicLong());
            }
        }
    }

    private static DimensionCounters countersOf(ResourceKey<Level> dimension) {
        return BY_DIMENSION.computeIfAbsent(dimension, key -> new DimensionCounters());
    }

    private static void count(ResourceKey<Level> dimension, String counter) {
        countersOf(dimension).counters.get(counter).incrementAndGet();
    }

    private static boolean firstDetail(ResourceKey<Level> dimension, String fold) {
        return countersOf(dimension).detailsLogged.add(fold);
    }

    public static void fold(ResourceKey<Level> dimension, String fold, BlockPos in, BlockPos out) {
        fold(dimension, fold, in.getX(), in.getZ(), out.getX(), out.getZ());
    }

    public static void fold(ResourceKey<Level> dimension, String fold, double inX, double inZ,
            double outX, double outZ) {
        count(dimension, fold + CALLS);
        if (inX == outX && inZ == outZ) {
            return;
        }

        count(dimension, fold + MOVED);
        if (firstDetail(dimension, fold)) {
            LOGGER.info("{} {} level={} in_x_blocks={} in_z_blocks={} out_x_blocks={} out_z_blocks={}",
                    TAG, fold, dimension.location(), inX, inZ, outX, outZ);
        }
    }

    // One axis at a time, for a fold whose vanilla arithmetic never assembles the two into a position — the explosion
    // reads its X and its Z through separate calls. The axis is a key rather than part of the name so both share one
    // pair of counters: the question is whether the knockback fold moved anything at all.
    public static void foldAxis(ResourceKey<Level> dimension, String fold, String axis, double in, double out) {
        count(dimension, fold + CALLS);
        if (in == out) {
            return;
        }

        count(dimension, fold + MOVED);
        if (firstDetail(dimension, fold + axis)) {
            LOGGER.info("{} {} level={} axis={} in_blocks={} out_blocks={}",
                    TAG, fold, dimension.location(), axis, in, out);
        }
    }

    public static void unwrapped(ResourceKey<Level> dimension, String fold) {
        count(dimension, fold + CALLS);
        count(dimension, fold + UNWRAPPED);
    }

    public static void noLevel(String fold) {
        LOGGER.info("{} no_level fold={} count={}", TAG, fold, NO_LEVEL.incrementAndGet());
    }

    // Gated on the level's own clock rather than on how often this is called: the caller runs once per player per
    // tick, so a call counter would fire once per player and read as several dumps of one interval.
    public static void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (gameTime % DUMP_INTERVAL_TICKS != 0) {
            return;
        }

        DimensionCounters counters = BY_DIMENSION.get(level.dimension());
        if (counters == null) {
            return;
        }

        long previous = counters.lastDumpedGameTime.get();
        if (previous == gameTime || !counters.lastDumpedGameTime.compareAndSet(previous, gameTime)) {
            return;
        }

        StringBuilder line = new StringBuilder(TAG)
                .append(" folds level=").append(level.dimension().location());
        for (String fold : FOLDS) {
            line.append(' ').append(fold).append(CALLS).append('=').append(counters.counters.get(fold + CALLS).get())
                    .append(' ').append(fold).append(MOVED).append('=').append(counters.counters.get(fold + MOVED).get())
                    .append(' ').append(fold).append(UNWRAPPED).append('=')
                    .append(counters.counters.get(fold + UNWRAPPED).get());
        }

        LOGGER.info(line.toString());
    }

    private ReshapeProbe() {
    }
}
