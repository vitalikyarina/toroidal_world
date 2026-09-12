package com.toroidalworld.engine.net;

import com.toroidalworld.core.CoordinateConstants;

public record PacketReach(String kind, double blocks, double slackBlocks) {
    private static final double MIRROR_SLACK = CoordinateConstants.CHUNK_WIDTH;

    private static final double TRACKED_SLACK = MIRROR_SLACK + CoordinateConstants.CHUNK_WIDTH;

    public static PacketReach measured(String kind, double blocks) {
        return new PacketReach(kind, blocks, MIRROR_SLACK);
    }

    public static PacketReach tracked(int viewDistance) {
        return new PacketReach(
                "tracked_entity", (double) viewDistance * CoordinateConstants.CHUNK_WIDTH, TRACKED_SLACK);
    }
}
