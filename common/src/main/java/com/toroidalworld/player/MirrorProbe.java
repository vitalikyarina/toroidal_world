package com.toroidalworld.player;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.WrapDomain;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

// Who moves the client mirror, and by how much. The mirror is the anchor the chunk translator folds every coordinate
// around, so a step it should not have taken does not stay a mirror problem — it decides which copy of a chunk the
// client is told to hold.
//
// The number that matters is not the size of the jump but what is left of it after whole worlds are taken off. A jump
// of exactly one world width leaves the mirror naming the same physical place, and every distance measured against it
// is unchanged, because the fold counts laps. A jump that lands part of a lap out moves the anchor for real, and every
// chunk translated against it afterwards is judged from the wrong place. Both are breaks; only the second one
// contaminates the readings the chunk doors are being judged by.
public final class MirrorProbe {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TAG = "[mirror-1211]";

    private static final int DUMP_INTERVAL_TICKS = 200;

    // How far off a whole lap a jump may land and still be read as a clean lap. A mover covers about a block between
    // two writes, so the slack is generous; it is a chunk wide because a chunk is the unit the anchor is read in, and
    // anything past it is already a whole chunk of error in every translation that follows.
    private static final double LAP_RESIDUAL_SLACK_BLOCKS = CoordinateConstants.CHUNK_WIDTH;

    private static final String WRITES_SUFFIX = "_writes";
    private static final String JUMP_SUFFIX = "_jumps";
    private static final String OFF_LAP_SUFFIX = "_jumps_off_lap";

    private static final String[] COUNTERS = counters();

    private static String[] counters() {
        MirrorWriter[] writers = MirrorWriter.values();
        String[] names = new String[writers.length * 3];
        int index = 0;
        for (MirrorWriter writer : writers) {
            names[index++] = writer.key() + WRITES_SUFFIX;
            names[index++] = writer.key() + JUMP_SUFFIX;
            names[index++] = writer.key() + OFF_LAP_SUFFIX;
        }
        return names;
    }

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

    private static void count(ResourceKey<Level> dimension, String counter) {
        BY_DIMENSION.computeIfAbsent(dimension, key -> new DimensionCounters())
                .counters.get(counter).incrementAndGet();
    }

    // An unseeded mirror has no dimension to file the write under; it is still worth a line, and there is nothing yet
    // for a counter to belong to.
    public static void write(@Nullable ResourceKey<Level> dimension, MirrorWriter writer, String axis,
            WrapDomain domain, double fromBlocks, double toBlocks, boolean jumped) {
        if (dimension == null) {
            return;
        }

        count(dimension, writer.key() + WRITES_SUFFIX);
        if (!jumped) {
            return;
        }

        count(dimension, writer.key() + JUMP_SUFFIX);

        double stepBlocks = toBlocks - fromBlocks;
        long laps = Math.round(stepBlocks / domain.domainLength);
        double residualBlocks = stepBlocks - (double) laps * domain.domainLength;
        boolean offLap = Math.abs(residualBlocks) > LAP_RESIDUAL_SLACK_BLOCKS;
        if (offLap) {
            count(dimension, writer.key() + OFF_LAP_SUFFIX);
        }

        if (!BY_DIMENSION.get(dimension).detailsLogged.add(writer.key() + '_' + axis)) {
            return;
        }

        LOGGER.warn("{} mirror_jump level={} writer={} axis={} from_blocks={} to_blocks={} step_blocks={}"
                        + " laps={} residual_blocks={} off_lap={} width_blocks={}",
                TAG, dimension.location(), writer.key(), axis, fromBlocks, toBlocks, stepBlocks,
                laps, residualBlocks, offLap ? 1 : 0, domain.domainLength);
    }

    // A rebase is the one legal far jump, so it is counted rather than judged — but it resets the frame, and a run
    // where the jumps and the rebases arrive in equal numbers is a different story from one where they do not.
    public static void rebase(ResourceKey<Level> dimension) {
        count(dimension, MirrorWriter.REBASE.key() + WRITES_SUFFIX);
    }

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
                .append(" writers level=").append(level.dimension().location());
        for (String counter : COUNTERS) {
            line.append(' ').append(counter).append('=').append(counters.counters.get(counter).get());
        }

        LOGGER.info(line.toString());
    }

    private MirrorProbe() {
    }
}
