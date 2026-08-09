package com.toroidalworld.net;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Diagnostics for the packet layer re-seated onto this game version. Everything the port had to rebuild reports here:
// the three packets swapped on the wire, the explosion's blown blocks, the position packet's two branches, and the
// teleport funnel's per-axis wrap — the last because its hook picks its argument by ordinal, which no compiler checks.
//
// Two shapes, for the two questions. A counter line per dimension, dumped on a game-time gate, answers "did this path
// run at all"; a single detail line per kind per dimension, the first time that kind actually moved a coordinate,
// answers "and did it move the coordinate the right way". A kind that ran but never moved anything shows as a counter
// with a zero beside it and no detail line, which is the shape of the defect this card exists to prevent.
//
// The server value each detail line prints is the packet's own, read before any translation, so the two numbers on the
// line can genuinely disagree.
public final class PacketProbe {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TAG = "[packet-1211]";

    // Long enough that a dump is not itself the log's bulk, short enough to bracket a single test step.
    private static final int DUMP_INTERVAL_TICKS = 200;

    private static final String TELEPORT_ENTITY = "teleport_entity";
    private static final String TELEPORT_ENTITY_MOVED = "teleport_entity_moved";
    private static final String TELEPORT_ENTITY_DROPPED = "teleport_entity_dropped";
    private static final String MOVE_VEHICLE = "move_vehicle";
    private static final String MOVE_VEHICLE_MOVED = "move_vehicle_moved";
    private static final String INTERACT = "interact";
    private static final String INTERACT_MOVED = "interact_moved";
    private static final String INTERACT_NO_LOCATION = "interact_no_location";
    private static final String EXPLODE = "explode";
    private static final String EXPLODE_MOVED = "explode_moved";
    private static final String ADD_ENTITY = "add_entity";
    private static final String ADD_ENTITY_MOVED = "add_entity_moved";
    private static final String PLAYER_POSITION_ABSOLUTE = "player_position_absolute";
    private static final String PLAYER_POSITION_RELATIVE = "player_position_relative";
    private static final String TELEPORT_WRAP_X = "teleport_wrap_x";
    private static final String TELEPORT_WRAP_Z = "teleport_wrap_z";
    private static final String TELEPORT_WRAP_X_MOVED = "teleport_wrap_x_moved";
    private static final String TELEPORT_WRAP_Z_MOVED = "teleport_wrap_z_moved";
    private static final String DIMENSION_BOUNDS = "dimension_bounds";

    // Seeded rather than created on first use, so a path that never ran prints a zero instead of vanishing from the
    // line — "it did not happen" and "I forgot to look" have to read differently.
    private static final String[] COUNTERS = {
            TELEPORT_ENTITY, TELEPORT_ENTITY_MOVED, TELEPORT_ENTITY_DROPPED,
            MOVE_VEHICLE, MOVE_VEHICLE_MOVED,
            INTERACT, INTERACT_MOVED, INTERACT_NO_LOCATION,
            EXPLODE, EXPLODE_MOVED,
            ADD_ENTITY, ADD_ENTITY_MOVED,
            PLAYER_POSITION_ABSOLUTE, PLAYER_POSITION_RELATIVE,
            TELEPORT_WRAP_X, TELEPORT_WRAP_Z, TELEPORT_WRAP_X_MOVED, TELEPORT_WRAP_Z_MOVED,
            DIMENSION_BOUNDS};

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

    private static boolean firstDetail(ResourceKey<Level> dimension, String kind) {
        return countersOf(dimension).detailsLogged.add(kind);
    }

    public static void teleportEntity(ResourceKey<Level> dimension, int entityId, Vec3 server, Vec3 client) {
        count(dimension, TELEPORT_ENTITY);
        if (server.x == client.x && server.z == client.z) {
            return;
        }

        count(dimension, TELEPORT_ENTITY_MOVED);
        if (firstDetail(dimension, TELEPORT_ENTITY)) {
            LOGGER.info("{} teleport_entity level={} entity={} server_x_blocks={} client_x_blocks={}"
                            + " server_z_blocks={} client_z_blocks={}",
                    TAG, dimension.location(), entityId, server.x, client.x, server.z, client.z);
        }
    }

    public static void teleportEntityDropped(ResourceKey<Level> dimension) {
        count(dimension, TELEPORT_ENTITY);
        count(dimension, TELEPORT_ENTITY_DROPPED);
    }

    public static void moveVehicle(ResourceKey<Level> dimension, Vec3 server, Vec3 client) {
        count(dimension, MOVE_VEHICLE);
        if (server.x == client.x && server.z == client.z) {
            return;
        }

        count(dimension, MOVE_VEHICLE_MOVED);
        if (firstDetail(dimension, MOVE_VEHICLE)) {
            LOGGER.info("{} move_vehicle level={} server_x_blocks={} client_x_blocks={}"
                            + " server_z_blocks={} client_z_blocks={}",
                    TAG, dimension.location(), server.x, client.x, server.z, client.z);
        }
    }

    public static void interactNoLocation(ResourceKey<Level> dimension) {
        count(dimension, INTERACT);
        count(dimension, INTERACT_NO_LOCATION);
    }

    public static void interact(ResourceKey<Level> dimension, int entityId, Vec3 client, Vec3 server) {
        count(dimension, INTERACT);
        if (client.x == server.x && client.z == server.z) {
            return;
        }

        count(dimension, INTERACT_MOVED);
        if (firstDetail(dimension, INTERACT)) {
            LOGGER.info("{} interact level={} entity={} client_x_blocks={} server_x_blocks={}"
                            + " client_z_blocks={} server_z_blocks={}",
                    TAG, dimension.location(), entityId, client.x, server.x, client.z, server.z);
        }
    }

    public static void explode(ResourceKey<Level> dimension, Vec3 serverCenter, Vec3 clientCenter,
            int blownCount, int shiftXBlocks, int shiftZBlocks) {
        count(dimension, EXPLODE);
        if (shiftXBlocks == 0 && shiftZBlocks == 0) {
            return;
        }

        count(dimension, EXPLODE_MOVED);
        if (firstDetail(dimension, EXPLODE)) {
            LOGGER.info("{} explode level={} server_x_blocks={} client_x_blocks={} server_z_blocks={}"
                            + " client_z_blocks={} blown_count={} shift_x_blocks={} shift_z_blocks={}",
                    TAG, dimension.location(), serverCenter.x, clientCenter.x, serverCenter.z, clientCenter.z,
                    blownCount, shiftXBlocks, shiftZBlocks);
        }
    }

    public static void addEntity(ResourceKey<Level> dimension, int entityId,
            double serverX, double clientX, double serverZ, double clientZ) {
        count(dimension, ADD_ENTITY);
        if (serverX == clientX && serverZ == clientZ) {
            return;
        }

        count(dimension, ADD_ENTITY_MOVED);
        if (firstDetail(dimension, ADD_ENTITY)) {
            LOGGER.info("{} add_entity level={} entity={} server_x_blocks={} client_x_blocks={}"
                            + " server_z_blocks={} client_z_blocks={}",
                    TAG, dimension.location(), entityId, serverX, clientX, serverZ, clientZ);
        }
    }

    // The two branches are counted apart because they are different code: a relative axis carries a folded delta, an
    // absolute one the nearest copy. A round that exercises only one of them leaves the other at zero, which says so.
    public static void playerPosition(ResourceKey<Level> dimension, boolean relativeX, boolean relativeZ,
            double packetX, double sentX, double packetZ, double sentZ, double mirrorX, double mirrorZ) {
        count(dimension, relativeX || relativeZ ? PLAYER_POSITION_RELATIVE : PLAYER_POSITION_ABSOLUTE);
        if (!firstDetail(dimension, relativeX || relativeZ ? PLAYER_POSITION_RELATIVE : PLAYER_POSITION_ABSOLUTE)) {
            return;
        }

        LOGGER.info("{} player_position level={} relative_x={} relative_z={} packet_x_blocks={} sent_x_blocks={}"
                        + " packet_z_blocks={} sent_z_blocks={} mirror_x_blocks={} mirror_z_blocks={}",
                TAG, dimension.location(), relativeX ? 1 : 0, relativeZ ? 1 : 0,
                packetX, sentX, packetZ, sentZ, mirrorX, mirrorZ);
    }

    // Per axis and per event: a teleport is rare, and this is the one hook whose binding nothing checks until it runs.
    // If the ordinal picked the wrong argument the in value on the x line reads as a height, not as an x.
    public static void teleportWrap(ResourceKey<Level> dimension, String axis, double in, double out) {
        boolean isX = "x".equals(axis);
        count(dimension, isX ? TELEPORT_WRAP_X : TELEPORT_WRAP_Z);
        if (in != out) {
            count(dimension, isX ? TELEPORT_WRAP_X_MOVED : TELEPORT_WRAP_Z_MOVED);
        }

        LOGGER.info("{} teleport_wrap level={} axis={} in_blocks={} out_blocks={}",
                TAG, dimension.location(), axis, in, out);
    }

    public static void teleportChunks(ResourceKey<Level> dimension, boolean wholeView, int flippedChunks) {
        LOGGER.info("{} teleport_chunks level={} whole_view={} flipped_chunks={}",
                TAG, dimension.location(), wholeView ? 1 : 0, flippedChunks);
    }

    // Fired from the tail of the dimension change, so the level named here is the one the player arrived in. A line
    // naming the level they left would mean the hook is sitting on the same-dimension branch instead.
    public static void dimensionBounds(ResourceKey<Level> dimension) {
        count(dimension, DIMENSION_BOUNDS);
        LOGGER.info("{} dimension_bounds level={}", TAG, dimension.location());
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
                .append(" rewrites level=").append(level.dimension().location());
        for (String counter : COUNTERS) {
            line.append(' ').append(counter).append('=').append(counters.counters.get(counter).get());
        }

        LOGGER.info(line.toString());
    }

    private PacketProbe() {
    }
}
