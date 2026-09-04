package com.toroidalworld.net;

import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WorldShapeSync {
    public static void sendTo(ServerPlayer player) {
        send(player, player.serverLevel());
    }

    public static void sendAllTo(ServerPlayer player) {
        for (ServerLevel level : player.serverLevel().getServer().getAllLevels()) {
            send(player, level);
        }
    }

    private static void send(ServerPlayer player, ServerLevel level) {
        FlatShape shape = ShapedChunkGenerator.wrappedShapeOf(level.getChunkSource().getGenerator());
        if (shape != null) {
            Platforms.get().sendWorldShape(player, level.dimension(), shape);
        }
    }

    private WorldShapeSync() {
    }
}
