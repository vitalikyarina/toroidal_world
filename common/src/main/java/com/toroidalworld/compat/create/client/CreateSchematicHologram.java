package com.toroidalworld.compat.create.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class CreateSchematicHologram {
    public static BlockPos atInit(BlockPos anchor) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, anchor);
    }

    public static BlockPos onTick(BlockPos target) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, target);
    }

    private CreateSchematicHologram() {
    }
}
