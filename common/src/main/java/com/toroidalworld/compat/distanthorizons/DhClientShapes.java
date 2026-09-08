package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.api.v1.ToroidalWorldClientApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

public final class DhClientShapes {
    static ToroidalShape of(Level level) {
        return level instanceof ClientLevel client ? ToroidalWorldClientApi.shapeOf(client).orElse(null) : null;
    }

    public static ToroidalShape ofCurrentLevel() {
        return of(Minecraft.getInstance().level);
    }

    private DhClientShapes() {
    }
}
