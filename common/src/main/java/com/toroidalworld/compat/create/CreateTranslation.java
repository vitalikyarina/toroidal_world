package com.toroidalworld.compat.create;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.toroidalworld.net.TagPositions;

public final class CreateTranslation {
    public static final String CONTROLLER_KEY = "Controller";
    public static final String LAST_KNOWN_POS_KEY = "LastKnownPos";
    private static final String PRINTER_KEY = "Printer";
    private static final String ANCHOR_KEY = "Anchor";
    private static final String FLYING_BLOCKS_KEY = "FlyingBlocks";
    private static final String TARGET_KEY = "Target";

    public static void register() {
        if (!CreateMod.present()) {
            return;
        }

        SyncedTagFold.register(IMultiBlockEntityContainer.class, TagPositions.PositionShape.BLOCK_POS,
                CONTROLLER_KEY, LAST_KNOWN_POS_KEY);
        SyncedTagFold.registerIn(SchematicannonBlockEntity.class, PRINTER_KEY,
                TagPositions.PositionShape.BLOCK_POS, ANCHOR_KEY);
        SyncedTagFold.registerInEach(SchematicannonBlockEntity.class, FLYING_BLOCKS_KEY,
                TagPositions.PositionShape.BLOCK_POS, TARGET_KEY);
    }

    private CreateTranslation() {
    }
}
