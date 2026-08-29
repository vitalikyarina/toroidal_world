package com.toroidalworld.compat.aeronautics.client;

import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class RopeAnchorFrame {
    public static BlockPos nearestCopy(BlockPos canonical) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return canonical;
        }

        return CreateSeamFold.foldPosition(level, player.blockPosition(), canonical);
    }

    private RopeAnchorFrame() {
    }
}
