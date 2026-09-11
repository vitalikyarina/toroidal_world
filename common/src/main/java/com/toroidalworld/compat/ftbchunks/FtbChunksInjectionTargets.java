package com.toroidalworld.compat.ftbchunks;

public final class FtbChunksInjectionTargets {
    public static final String XZ_REGION_FROM_CHUNK =
            "Ldev/ftb/mods/ftblibrary/math/XZ;regionFromChunk(II)Ldev/ftb/mods/ftblibrary/math/XZ;";

    public static final String XZ_OF =
            "Ldev/ftb/mods/ftblibrary/math/XZ;of(II)Ldev/ftb/mods/ftblibrary/math/XZ;";

    public static final String CHUNK_BUTTON_CHUNK_POS =
            "Ldev/ftb/mods/ftbchunks/client/gui/ChunkScreenPanel$ChunkButton;chunkPos:"
                    + "Ldev/ftb/mods/ftblibrary/math/XZ;";

    public static final String REQUEST_CHUNK_CHANGE_PACKET_INIT =
            "Ldev/ftb/mods/ftbchunks/net/RequestChunkChangePacket;<init>("
                    + "Ldev/ftb/mods/ftbchunks/net/RequestChunkChangePacket$ChunkChangeOp;"
                    + "Ljava/util/Set;ZLjava/util/Optional;)V";

    public static final String MAP_REGION_DATA_GET_CHUNK =
            "Ldev/ftb/mods/ftbchunks/client/map/MapRegionData;getChunk(Ldev/ftb/mods/ftblibrary/math/XZ;)"
                    + "Ldev/ftb/mods/ftbchunks/client/map/MapChunk;";

    private FtbChunksInjectionTargets() {
    }
}
