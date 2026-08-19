package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.client.multiplayer.ClientLevel;

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
