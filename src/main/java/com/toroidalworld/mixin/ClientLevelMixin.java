package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.client.multiplayer.ClientLevel;

// Until the server's bounds payload arrives the holder answers NOOP, so a bounds reader honestly sees an unwrapped
// world — which is also the truth on a server that never sends the payload. A fresh level (a dimension change) starts
// back at NOOP the same way.
@Mixin(ClientLevel.class)
public class ClientLevelMixin implements ClientBoundsHolder {
    @Unique
    private WorldLoopTransformer toroidal$clientBounds = WorldLoopTransformer.NOOP;

    @Override
    public WorldLoopTransformer toroidal$clientBounds() {
        return this.toroidal$clientBounds;
    }

    @Override
    public void toroidal$setClientBounds(WorldLoopTransformer transformer) {
        this.toroidal$clientBounds = transformer;
    }
}
