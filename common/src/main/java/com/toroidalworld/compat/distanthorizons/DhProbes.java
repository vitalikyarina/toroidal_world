package com.toroidalworld.compat.distanthorizons;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.api.v1.ToroidalShape;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;

import net.minecraft.core.Direction;

public final class DhProbes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NONE = "none";

    private static final Set<String> SEEN_KEY_PERIODS = ConcurrentHashMap.newKeySet();

    enum Key {
        SECTION("section"),
        CHUNK("chunk"),
        BEACON("beacon");

        private final String label;
        private final AtomicInteger lines = new AtomicInteger();
        private final LongAdder kept = new LongAdder();

        Key(String label) {
            this.label = label;
        }

        private boolean firstFold() {
            return this.lines.get() == 0 && this.lines.getAndIncrement() == 0;
        }
    }

    public static void keyPeriod(ToroidalShape shape, byte leafDetailLevel) {
        String widthX = widthValue(shape, Direction.Axis.X);
        String widthZ = widthValue(shape, Direction.Axis.Z);
        if (!SEEN_KEY_PERIODS.add(widthX + ":" + widthZ)) {
            return;
        }

        LOGGER.info("[dh-compat] key_period width_x_blocks={} period_x_blocks={} laps_x={}"
                + " width_z_blocks={} period_z_blocks={} laps_z={}",
                widthX, periodValue(shape, Direction.Axis.X, leafDetailLevel),
                lapsValue(shape, Direction.Axis.X, leafDetailLevel),
                widthZ, periodValue(shape, Direction.Axis.Z, leafDetailLevel),
                lapsValue(shape, Direction.Axis.Z, leafDetailLevel));
    }

    static String widthValue(ToroidalShape shape, Direction.Axis axis) {
        return shape.loops(axis) ? String.valueOf(shape.widthBlocks(axis)) : NONE;
    }

    static String periodValue(ToroidalShape shape, Direction.Axis axis, byte leafDetailLevel) {
        return shape.loops(axis) ? String.valueOf(DhFold.periodBlocks(shape, axis, leafDetailLevel)) : NONE;
    }

    static String lapsValue(ToroidalShape shape, Direction.Axis axis, byte leafDetailLevel) {
        return shape.loops(axis)
                ? String.valueOf(DhFold.periodBlocks(shape, axis, leafDetailLevel) / shape.widthBlocks(axis))
                : NONE;
    }

    public static void repoShape(Object repo, IDhLevel level, boolean present) {
        LOGGER.info("[dh-compat] repo_shape repo={} level={} shape={}",
                repo.getClass().getSimpleName(), levelName(level), present ? "present" : "absent");
    }

    private static String levelName(IDhLevel level) {
        return level == null ? NONE : level.getLevelWrapper().getDhIdentifier();
    }

    static void sectionKeyFolded(long raw, long folded) {
        if (Key.SECTION.firstFold()) {
            LOGGER.info(foldedKeyLine(Key.SECTION, DhSectionPos.toString(raw), DhSectionPos.toString(folded)));
        }
    }

    static void chunkKeyFolded(int rawX, int rawZ, int foldedX, int foldedZ) {
        if (Key.CHUNK.firstFold()) {
            LOGGER.info(foldedKeyLine(Key.CHUNK, chunkValue(rawX, rawZ), chunkValue(foldedX, foldedZ)));
        }
    }

    static void beaconKeyFolded(DhBlockPos raw, DhBlockPos folded) {
        if (Key.BEACON.firstFold()) {
            LOGGER.info(foldedKeyLine(Key.BEACON, beaconValue(raw), beaconValue(folded)));
        }
    }

    static void keyKept(Key key) {
        key.kept.increment();
    }

    static String foldedKeyLine(Key key, String raw, String folded) {
        return "[dh-compat] folded_key key_type=" + key.label
                + " raw=" + raw + " folded=" + folded + " unchanged_keys=" + key.kept.sum();
    }

    static String chunkValue(int x, int z) {
        return x + "," + z;
    }

    static String beaconValue(DhBlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    static int foldedKeyLines(Key key) {
        return key.lines.get();
    }

    static long unchangedKeys(Key key) {
        return key.kept.sum();
    }

    static void resetKeyGates() {
        for (Key key : Key.values()) {
            key.lines.set(0);
            key.kept.reset();
        }
    }

    private DhProbes() {
    }
}
