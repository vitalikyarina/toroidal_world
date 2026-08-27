package com.toroidalworld.net;

import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.server.level.ServerPlayer;

public final class WorldShapeSync {
    public static void sendTo(ServerPlayer player) {
        FlatShape shape = ShapedChunkGenerator.wrappedShapeOf(player.serverLevel().getChunkSource().getGenerator());
        if (shape != null) {
            Platforms.get().sendWorldShape(player, shape);
        }
    }

    private WorldShapeSync() {
    }
}
