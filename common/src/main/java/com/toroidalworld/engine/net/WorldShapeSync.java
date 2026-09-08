package com.toroidalworld.engine.net;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.platform.Platforms;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WorldShapeSync {
    public static void sendTo(ServerPlayer player) {
        send(player, player.level());
    }

    public static void sendAllTo(ServerPlayer player) {
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
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
