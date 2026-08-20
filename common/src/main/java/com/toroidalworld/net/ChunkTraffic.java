package com.toroidalworld.net;

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
