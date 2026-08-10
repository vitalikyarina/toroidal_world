package com.toroidalworld.player;

// Who moved the client mirror. The mirror is the anchor every chunk and coordinate translation is folded around, and
// four different places write it — so when it steps further than it may, the step has to name its author or the log
// says only that one of the four did it.
public enum MirrorWriter {
    PLAYER_MOVE("player_move"),
    VEHICLE_MOVE("vehicle_move"),
    POSITION_PACKET("position_packet"),
    REBASE("rebase");

    private final String key;

    MirrorWriter(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
