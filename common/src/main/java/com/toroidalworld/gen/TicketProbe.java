package com.toroidalworld.gen;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

// Diagnostics for the ticket-key fold re-seated onto this game version, where a raw key enters TWO graphs instead of
// one. The question this has to answer is not "does the fold run" but "did every graph get the folded key" — so the
// two graphs are counted apart, under names that read side by side on one line.
//
// Three counters per graph and operation. raw_oob is how many keys arrived past the bounds: it is the round's own
// proof that the seam was exercised at all, and a round where a graph's raw_oob is zero proves nothing about that
// graph. post_fold_oob is how many keys were STILL past the bounds after folding, and is the failure signal — it must
// read zero on every line. A first detail line per graph and operation prints the coordinates of the first key the
// fold actually moved, so "it ran" and "it moved the coordinate the right way" cannot be confused for each other.
//
// tt_add_oob is the counter this card exists for: on 26.2 that graph was fed from inside the single folded primitive,
// so nothing could have arrived raw. Here it is fed by a separate call, and a build folding only the distance manager
// would leave this number equal to tt_add's own out-of-bounds share with nothing folding it.
public final class TicketProbe {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TAG = "[ticket-1211]";

    // Long enough that a dump is not itself the log's bulk, short enough to bracket a single test step.
    private static final int DUMP_INTERVAL_TICKS = 200;

    public static final String GRAPH_DISTANCE_MANAGER = "dm";
    public static final String GRAPH_TICKING_TRACKER = "tt";
    public static final String OP_ADD = "add";
    public static final String OP_REMOVE = "remove";

    private static final String OOB_SUFFIX = "_oob";
    private static final String POST_FOLD_OOB = "post_fold_oob";
    private static final String HOLDER_SCHEDULED = "holder_scheduled";
    private static final String HOLDER_SCHEDULED_OOB = "holder_scheduled_oob";

    // Seeded rather than created on first use, so a path that never ran prints a zero instead of vanishing from the
    // line — "it did not happen" and "I forgot to look" have to read differently.
    private static final String[] COUNTERS = {
            "dm_add", "dm_add_oob", "dm_remove", "dm_remove_oob",
            "tt_add", "tt_add_oob", "tt_remove", "tt_remove_oob",
            POST_FOLD_OOB, HOLDER_SCHEDULED, HOLDER_SCHEDULED_OOB};

    private static final Map<ResourceKey<Level>, DimensionCounters> BY_DIMENSION = new ConcurrentHashMap<>();

    private static final class DimensionCounters {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        private final Set<String> detailsLogged = ConcurrentHashMap.newKeySet();
        private final AtomicLong lastDumpedGameTime = new AtomicLong(-1L);

        private DimensionCounters() {
            for (String counter : COUNTERS) {
                counters.put(counter, new AtomicLong());
            }
        }
    }

    private static DimensionCounters countersOf(ResourceKey<Level> dimension) {
        return BY_DIMENSION.computeIfAbsent(dimension, key -> new DimensionCounters());
    }

    private static void count(ResourceKey<Level> dimension, String counter) {
        countersOf(dimension).counters.get(counter).incrementAndGet();
    }

    // The raw key and the folded one are two numbers this method is handed, not two readings taken through a primitive
    // the mod folds — the caller has both in hand at the moment it substitutes one for the other, so they can genuinely
    // disagree, and a build that stopped folding would print them equal.
    public static void folded(ServerLevel level, WorldLoopTransformer transformer, String graph, String operation,
            long rawKey, long foldedKey) {
        ResourceKey<Level> dimension = level.dimension();
        String counter = graph + "_" + operation;
        count(dimension, counter);

        if (!isOutOfBounds(transformer, rawKey)) {
            return;
        }

        count(dimension, counter + OOB_SUFFIX);
        if (isOutOfBounds(transformer, foldedKey)) {
            count(dimension, POST_FOLD_OOB);
        }

        if (!countersOf(dimension).detailsLogged.add(counter)) {
            return;
        }

        LOGGER.info("{} fold level={} graph={} op={} raw_chunk_x={} raw_chunk_z={} folded_chunk_x={} folded_chunk_z={}",
                TAG, dimension.location(), graph, operation,
                ChunkPos.getX(rawKey), ChunkPos.getZ(rawKey), ChunkPos.getX(foldedKey), ChunkPos.getZ(foldedKey));
    }

    // "A chunk past the bounds never gets a holder" is the DoD clause this counts. Every key the scheduler answers for
    // is counted; the out-of-bounds share must stay zero, because with both graphs folded no such key is ever
    // enumerated in the first place.
    public static void holderScheduled(ServerLevel level, WorldLoopTransformer transformer, long key, boolean created) {
        if (!created) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        count(dimension, HOLDER_SCHEDULED);
        if (!isOutOfBounds(transformer, key)) {
            return;
        }

        count(dimension, HOLDER_SCHEDULED_OOB);
        if (countersOf(dimension).detailsLogged.add(HOLDER_SCHEDULED_OOB)) {
            LOGGER.info("{} phantom_holder level={} chunk_x={} chunk_z={}",
                    TAG, dimension.location(), ChunkPos.getX(key), ChunkPos.getZ(key));
        }
    }

    // The pair of chunks adjacent through the +X seam, at the Z the player stands on. Both coordinates are inside the
    // bounds, so nothing folds on the way in and the two levels are read exactly as the graphs stored them. A ticket
    // level that spread across the seam reads within one step on the two sides; an unfolded graph leaves the far side
    // at its unloaded sentinel while the near side is loaded.
    public static void seamLevels(ServerLevel level, String player, int chunkZ, int maxChunkX, int minChunkX,
            int ticketMax, int ticketMin, int tickingMax, int tickingMin) {
        LOGGER.info("{} seam_level level={} player={} chunk_z={} seam_max_chunk_x={} seam_min_chunk_x={}"
                        + " ticket_level_max={} ticket_level_min={} ticking_level_max={} ticking_level_min={}",
                TAG, level.dimension().location(), player, chunkZ, maxChunkX, minChunkX,
                ticketMax, ticketMin, tickingMax, tickingMin);
    }

    // Gated on the level's own clock rather than on how often this is called: runAllUpdates runs more than once per
    // tick, so a call counter would fire several times per second and flood the log.
    public static boolean shouldDump(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (gameTime % DUMP_INTERVAL_TICKS != 0) {
            return false;
        }

        DimensionCounters counters = BY_DIMENSION.get(level.dimension());
        if (counters == null) {
            return false;
        }

        long previous = counters.lastDumpedGameTime.get();
        if (previous == gameTime || !counters.lastDumpedGameTime.compareAndSet(previous, gameTime)) {
            return false;
        }

        StringBuilder line = new StringBuilder(TAG).append(" fold_census level=").append(level.dimension().location());
        for (String counter : COUNTERS) {
            line.append(' ').append(counter).append('=').append(counters.counters.get(counter).get());
        }

        LOGGER.info(line.toString());
        return true;
    }

    private static boolean isOutOfBounds(WorldLoopTransformer transformer, long chunkKey) {
        return transformer.chunks.x.isOver(ChunkPos.getX(chunkKey))
                || transformer.chunks.z.isOver(ChunkPos.getZ(chunkKey));
    }

    private TicketProbe() {
    }
}
