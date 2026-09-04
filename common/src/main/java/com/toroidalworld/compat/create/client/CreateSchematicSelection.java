package com.toroidalworld.compat.create.client;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.create.CreateSchematicFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class CreateSchematicSelection {
    private static final String OVERSIZED_SELECTION_KEY = "message.toroidal_world.schematic.selection_wider_than_world";
    private static final String OVERSIZED_GROWTH_KEY = "message.toroidal_world.schematic.selection_cannot_grow_past_world";

    public static boolean refuseOversizedCorner(@Nullable BlockPos first, @Nullable BlockPos second) {
        return refuseOversized(first, second, OVERSIZED_SELECTION_KEY);
    }

    public static boolean refuseOversizedGrowth(@Nullable BlockPos first, @Nullable BlockPos second) {
        return refuseOversized(first, second, OVERSIZED_GROWTH_KEY);
    }

    private static boolean refuseOversized(@Nullable BlockPos first, @Nullable BlockPos second, String messageKey) {
        if (first == null || second == null
                || !CreateSchematicFold.regionExceedsWorld(Minecraft.getInstance().level, first, second)) {
            return false;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }

        return true;
    }

    private CreateSchematicSelection() {
    }
}
