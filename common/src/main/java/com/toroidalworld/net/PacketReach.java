package com.toroidalworld.net;

import com.toroidalworld.core.CoordinateConstants;

public record PacketReach(String kind, double blocks, double slackBlocks) {
    private static final double MIRROR_SLACK = CoordinateConstants.CHUNK_WIDTH;

    private static final double TRACKED_SLACK = MIRROR_SLACK + CoordinateConstants.CHUNK_WIDTH;

    public static final PacketReach PARTICLE = new PacketReach("particle", 32.0, MIRROR_SLACK);
    public static final PacketReach FORCED_PARTICLE = new PacketReach("forced_particle", 512.0, MIRROR_SLACK);

    public static final PacketReach EXPLOSION = new PacketReach("explosion", 64.0, MIRROR_SLACK);

    public static PacketReach sound(float range) {
        return new PacketReach("sound", range, MIRROR_SLACK);
    }

    public static PacketReach tracked(int viewDistance) {
        return new PacketReach(
                "tracked_entity", (double) viewDistance * CoordinateConstants.CHUNK_WIDTH, TRACKED_SLACK);
    }
}
