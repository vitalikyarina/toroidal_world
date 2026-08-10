package com.toroidalworld.probe;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Diagnostics for the folds re-seated onto 1.21.1 whose vanilla anchor moved rather than changed shape — the renames,
// the two reshaped bodies, and the twenty-three lambda indices vanilla renumbered.
//
// Mixin apply already proves the anchor exists: a handler that finds nothing kills the process, so a game that reaches
// the main menu has resolved every string. What it cannot prove is that the anchor is on the path the behaviour
// actually walks. A lambda index chosen because that lambda carries the injected call is a derivation, not a
// measurement, and the failure it can still hide is silent: the fold applies, the behaviour runs somewhere else, and
// nothing in the log says so.
//
// So the reading is per fold: calls, and of those the ones where the folded answer differed from the vanilla one. A
// fold whose calls climb while moved stays at zero on a wrapped level is a fold sitting on ground the seam never
// crosses; calls at zero after the behaviour was staged is a fold on the wrong path entirely. The unwrapped counter
// separates the third case — the fold ran where doing nothing is correct.
//
// The vanilla side is the operation's own untouched result, taken by calling it, not a second reading through a
// primitive this mod folds: asking a wrapped method for the raw answer measures the fix against itself.
public final class ReseatProbe {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TAG = "[reseat-1211]";

    private static final int DUMP_INTERVAL_TICKS = 200;

    // The world border holds no reference back to its level, so its fold has no dimension to file under.
    private static final String NO_LEVEL = "none";

    public static final String TURTLE_HOME = "turtle_home";
    public static final String ENTITY_CHUNK_KEY = "entity_chunk_key";
    public static final String BORDER_CLAMP = "border_clamp";
    public static final String KNOCKBACK_DIR = "knockback_dir";
    public static final String GHAST_AIM = "ghast_aim";
    public static final String LEASH_ELASTIC = "leash_elastic";
    public static final String BOAT_LEASH = "boat_leash";

    public static final String POI_CLAIM = "poi_claim";
    public static final String JOB_SITE_REACH = "job_site_reach";
    public static final String BELL_RAIDER_RANGE = "bell_raider_range";
    public static final String ARRIVAL_GATE = "arrival_gate";
    public static final String DOOR_REACH = "door_reach";
    public static final String DOOR_OTHER_MOB = "door_other_mob";
    public static final String HIDING_PLACE_REACH = "hiding_place_reach";
    public static final String DETECTOR_RANGE = "detector_range";
    public static final String BELL_REACH = "bell_reach";
    public static final String HOME_DISTANCE = "home_distance";
    public static final String HIDDEN_STATE_REACH = "hidden_state_reach";
    public static final String AVOID_REACH = "avoid_reach";
    public static final String AVOID_HEADING = "avoid_heading";
    public static final String MEMORY_DISTANCE = "memory_distance";
    public static final String MEETING_POINT_REACH = "meeting_point_reach";
    public static final String FOLLOW_REACH = "follow_reach";
    public static final String POI_TETHER_AROUND = "poi_tether_around";
    public static final String ANCHOR_TETHER = "anchor_tether";
    public static final String POI_TETHER = "poi_tether";
    public static final String POI_IN_RANGE = "poi_in_range";
    public static final String WARNING_RANGE = "warning_range";

    // The folds whose sugar was re-seated: their level or their named locals moved between 26.x and 1.21.1, so what
    // needs proving here is not that the anchor resolves but that the sugar hands them the quantity they think it does.
    public static final String RAID_HORN = "raid_horn";
    public static final String RAIDER_DISTANCE = "raider_distance";
    public static final String VILLAGE_SECTIONS = "village_sections";
    public static final String NEAREST_VILLAGE = "nearest_village";
    public static final String RING_SEARCH = "ring_search";

    private static final String[] FOLDS = {
            TURTLE_HOME, ENTITY_CHUNK_KEY, BORDER_CLAMP, KNOCKBACK_DIR, GHAST_AIM, LEASH_ELASTIC, BOAT_LEASH,
            POI_CLAIM, JOB_SITE_REACH, BELL_RAIDER_RANGE, ARRIVAL_GATE, DOOR_REACH, DOOR_OTHER_MOB,
            HIDING_PLACE_REACH, DETECTOR_RANGE, BELL_REACH, HOME_DISTANCE, HIDDEN_STATE_REACH,
            AVOID_REACH, AVOID_HEADING, MEMORY_DISTANCE, MEETING_POINT_REACH, FOLLOW_REACH,
            POI_TETHER_AROUND, ANCHOR_TETHER, POI_TETHER, POI_IN_RANGE, WARNING_RANGE,
            RAID_HORN, RAIDER_DISTANCE, VILLAGE_SECTIONS, NEAREST_VILLAGE, RING_SEARCH};

    private static final String CALLS = "_calls";
    private static final String MOVED = "_moved";
    private static final String UNWRAPPED = "_unwrapped";

    private static final Map<String, LevelCounters> BY_LEVEL = new ConcurrentHashMap<>();

    private static final class LevelCounters {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        private final Set<String> detailsLogged = ConcurrentHashMap.newKeySet();
        private final AtomicLong lastDumpedGameTime = new AtomicLong(-1L);

        // Seeded rather than created on first use, so a fold that never ran prints a zero instead of vanishing from
        // the line — "it did not happen" and "I forgot to look" have to read differently.
        private LevelCounters() {
            for (String fold : FOLDS) {
                counters.put(fold + CALLS, new AtomicLong());
                counters.put(fold + MOVED, new AtomicLong());
                counters.put(fold + UNWRAPPED, new AtomicLong());
            }
        }
    }

    private static String nameOf(@Nullable Level level) {
        return level == null ? NO_LEVEL : level.dimension().location().toString();
    }

    private static void count(String levelName, String counter) {
        BY_LEVEL.computeIfAbsent(levelName, key -> new LevelCounters()).counters.get(counter).incrementAndGet();
    }

    private static boolean firstDetail(String levelName, String fold) {
        return BY_LEVEL.computeIfAbsent(levelName, key -> new LevelCounters()).detailsLogged.add(fold);
    }

    // True once the fold has been counted as running on a level that does not loop, where leaving the value alone is
    // the correct answer and the caller has nothing further to record.
    private static boolean countedAsUnwrapped(@Nullable Level level, String fold) {
        String levelName = nameOf(level);
        count(levelName, fold + CALLS);
        if (level != null && WorldLoopAttachments.wrappedTransformerOf(level) == null) {
            count(levelName, fold + UNWRAPPED);
            return true;
        }

        return false;
    }

    private static void moved(@Nullable Level level, String fold, String detail) {
        String levelName = nameOf(level);
        count(levelName, fold + MOVED);
        if (firstDetail(levelName, fold)) {
            LOGGER.info("{} {} level={} {}", TAG, fold, levelName, detail);
        }
    }

    public static boolean decided(@Nullable Level level, String fold, boolean vanilla, boolean folded) {
        if (!countedAsUnwrapped(level, fold) && vanilla != folded) {
            moved(level, fold, "vanilla=" + vanilla + " folded=" + folded);
        }

        return folded;
    }

    // The unit travels with the call because these folds do not share one: a squared distance, a Manhattan sum and a
    // plain offset all arrive here, and a bare number in the log is a number nobody can check.
    public static double decided(@Nullable Level level, String fold, String unit, double vanilla, double folded) {
        if (!countedAsUnwrapped(level, fold) && vanilla != folded) {
            moved(level, fold, "vanilla_" + unit + "=" + vanilla + " folded_" + unit + "=" + folded);
        }

        return folded;
    }

    public static int decided(@Nullable Level level, String fold, String unit, int vanilla, int folded) {
        return (int) decided(level, fold, unit, (double) vanilla, (double) folded);
    }

    public static long decidedChunkKey(@Nullable Level level, String fold, long vanilla, long folded) {
        if (!countedAsUnwrapped(level, fold) && vanilla != folded) {
            moved(level, fold, "vanilla_chunk_key=" + vanilla + " folded_chunk_key=" + folded);
        }

        return folded;
    }

    public static BlockPos decided(@Nullable Level level, String fold, BlockPos vanilla, BlockPos folded) {
        if (!countedAsUnwrapped(level, fold) && !vanilla.equals(folded)) {
            moved(level, fold, "vanilla_x_blocks=" + vanilla.getX() + " vanilla_z_blocks=" + vanilla.getZ()
                    + " folded_x_blocks=" + folded.getX() + " folded_z_blocks=" + folded.getZ());
        }

        return folded;
    }

    public static Vec3 decided(@Nullable Level level, String fold, Vec3 vanilla, Vec3 folded) {
        if (!countedAsUnwrapped(level, fold) && !vanilla.equals(folded)) {
            moved(level, fold, "vanilla_x_blocks=" + vanilla.x + " vanilla_z_blocks=" + vanilla.z
                    + " folded_x_blocks=" + folded.x + " folded_z_blocks=" + folded.z);
        }

        return folded;
    }

    // Gated on the level's own clock rather than on how often this is called: the caller runs once per player per
    // tick, so a call counter would fire once per player and read as several dumps of one interval.
    public static void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (gameTime % DUMP_INTERVAL_TICKS != 0) {
            return;
        }

        dump(nameOf(level), gameTime);
        if (level.dimension() == Level.OVERWORLD) {
            dump(NO_LEVEL, gameTime);
        }
    }

    private static void dump(String levelName, long gameTime) {
        LevelCounters counters = BY_LEVEL.get(levelName);
        if (counters == null) {
            return;
        }

        long previous = counters.lastDumpedGameTime.get();
        if (previous == gameTime || !counters.lastDumpedGameTime.compareAndSet(previous, gameTime)) {
            return;
        }

        StringBuilder line = new StringBuilder(TAG).append(" folds level=").append(levelName);
        for (String fold : FOLDS) {
            line.append(' ').append(fold).append(CALLS).append('=').append(counters.counters.get(fold + CALLS).get())
                    .append(' ').append(fold).append(MOVED).append('=').append(counters.counters.get(fold + MOVED).get())
                    .append(' ').append(fold).append(UNWRAPPED).append('=')
                    .append(counters.counters.get(fold + UNWRAPPED).get());
        }

        LOGGER.info(line.toString());
    }

    private ReseatProbe() {
    }
}
