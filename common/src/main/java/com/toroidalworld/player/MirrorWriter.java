package com.toroidalworld.player;

public enum MirrorWriter {
    PLAYER_MOVE("player_move", true),
    VEHICLE_MOVE("vehicle_move", true),
    POSITION_PACKET("position_packet", false);

    private final String key;
    private final boolean clientAuthored;

    MirrorWriter(String key, boolean clientAuthored) {
        this.key = key;
        this.clientAuthored = clientAuthored;
    }

    public String key() {
        return key;
    }

    public boolean clientAuthored() {
        return clientAuthored;
    }
}
