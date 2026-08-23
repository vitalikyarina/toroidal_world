package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class CreateEquipmentFold {
    public static @Nullable BlockPos canonicalisePacketPosition(@Nullable Level level, @Nullable BlockPos raw) {
        return raw == null ? null : CreateSeamFold.canonical(level, raw);
    }

    private CreateEquipmentFold() {
    }
}
