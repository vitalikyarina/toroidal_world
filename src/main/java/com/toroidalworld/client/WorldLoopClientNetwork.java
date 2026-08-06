package com.toroidalworld.client;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// Stores the bounds the server sent in the client-only CLIENT_BOUNDS_TRANSFORMER, NOT the level's DIMENSION_TRANSFORMER:
// that one drives the wrapping engine, and enabling it on the client would make the client believe the world is finite,
// which breaks chunk loading and rendering across the seam. The bounds are held apart so the overlay can read them
// without the level ever wrapping. Until this arrives the store is NOOP, so the overlay honestly shows the raw
// client position — which is where the player really thinks they are.
@OnlyIn(Dist.CLIENT)
public final class WorldLoopClientNetwork {
    public static void apply(WorldLoopBounds settings) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            level.setData(WorldLoopAttachments.CLIENT_BOUNDS_TRANSFORMER, new WorldLoopTransformer(settings));
        }
    }

    private WorldLoopClientNetwork() {
    }
}
