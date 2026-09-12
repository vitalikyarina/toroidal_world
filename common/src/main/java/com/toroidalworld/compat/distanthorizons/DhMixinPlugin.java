package com.toroidalworld.compat.distanthorizons;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.ModPresenceGatePlugin;
import com.toroidalworld.compat.ModSymbol;

public class DhMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol LEVEL_CHUNK_HASH_REPO = new ModSymbol(
            "com/seibel/distanthorizons/core/level/AbstractDhLevel", "chunkHashRepo",
            "Lcom/seibel/distanthorizons/core/sql/repo/ChunkHashRepo;");

    private static final ModPresence DH = ModPresence.of(LOGGER,
            "com/seibel/distanthorizons/core/api/internal/ClientApi.class",
            "[dh-compat] gate distanthorizons_present", LEVEL_CHUNK_HASH_REPO);

    public DhMixinPlugin() {
        super(DH);
    }
}
