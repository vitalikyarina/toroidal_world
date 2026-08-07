package com.toroidalworld.platform;

import com.toroidalworld.config.WorldLoopConfig;
import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgePlatform implements Platform {
    @Override
    public boolean isClient() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }

    @Override
    public void sendWrappingBounds(ServerPlayer player, WorldLoopBounds bounds) {
        PacketDistributor.sendToPlayer(player, new WrappingSettingsPayload(bounds));
    }

    @Override
    public boolean showRawF3Coordinates() {
        return WorldLoopConfig.SHOW_RAW_F3_COORDINATES.get();
    }
}
