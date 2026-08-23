package com.toroidalworld.compat.create.client;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.create.CreateSchematicFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class CreateSchematicSelection {
    private static final String OVERSIZED_SELECTION_KEY = "message.toroidal_world.schematic.selection_wider_than_world";

    public static boolean refuseOversized(@Nullable BlockPos first, @Nullable BlockPos second) {
        if (first == null || second == null
                || !CreateSchematicFold.regionExceedsWorld(Minecraft.getInstance().level, first, second)) {
            return false;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.translatable(OVERSIZED_SELECTION_KEY), true);
        }

        return true;
    }

    private CreateSchematicSelection() {
    }
}
