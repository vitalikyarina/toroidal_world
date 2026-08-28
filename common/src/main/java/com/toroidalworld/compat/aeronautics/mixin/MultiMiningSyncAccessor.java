package com.toroidalworld.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.ryanhcode.offroad.network.borehead_bearing.ClientboundMultiMiningSync;

@Mixin(value = ClientboundMultiMiningSync.class, remap = false)
public interface MultiMiningSyncAccessor {
    @Accessor("breakingID")
    int toroidal$breakingId();
}
