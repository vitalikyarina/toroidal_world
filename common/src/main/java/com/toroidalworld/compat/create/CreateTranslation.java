package com.toroidalworld.compat.create;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.toroidalworld.net.TagPositions;

public final class CreateTranslation {
    public static final String CONTROLLER_KEY = "Controller";
    public static final String LAST_KNOWN_POS_KEY = "LastKnownPos";

    public static void register() {
        if (!CreateMod.present()) {
            return;
        }

        SyncedTagFold.register(IMultiBlockEntityContainer.class, TagPositions.PositionShape.BLOCK_POS,
                CONTROLLER_KEY, LAST_KNOWN_POS_KEY);
    }

    private CreateTranslation() {
    }
}
