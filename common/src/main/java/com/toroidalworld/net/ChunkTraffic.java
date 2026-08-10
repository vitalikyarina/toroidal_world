package com.toroidalworld.net;

// Which door a chunk coordinate arrived through, carried alongside it the way PacketReach is carried on the
// loose-coordinate door. A guard that fires can then name the traffic that produced the key instead of only the number
// it disliked — the same question the loose-coordinate guard answers with reach.kind().
//
// Unlike PacketReach this carries no radius of its own: every packet here reaches the client because it names a chunk
// the client is holding, so they all answer to the one bound the view sets.
public enum ChunkTraffic {
    CHUNK_DATA("chunk_data"),
    LIGHT_UPDATE("light_update"),
    AUX_LIGHT("aux_light"),
    CHUNK_BIOMES("chunk_biomes"),
    CACHE_CENTER("cache_center"),
    FORGET("forget"),
    SECTION_BLOCKS("section_blocks"),
    BLOCK_UPDATE("block_update"),
    BLOCK_ENTITY("block_entity"),
    BLOCK_DESTRUCTION("block_destruction"),
    BLOCK_EVENT("block_event"),
    LEVEL_EVENT("level_event"),
    SIGN_EDITOR("sign_editor"),
    ENTITY_DATA("entity_data"),
    BLOCK_PARTICLE("block_particle");

    private final String key;

    ChunkTraffic(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
