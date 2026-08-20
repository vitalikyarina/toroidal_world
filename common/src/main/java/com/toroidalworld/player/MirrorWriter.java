package com.toroidalworld.player;

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
