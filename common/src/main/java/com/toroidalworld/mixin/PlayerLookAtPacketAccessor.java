package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;

@Mixin(ClientboundPlayerLookAtPacket.class)
public interface PlayerLookAtPacketAccessor {
    @Accessor("x")
    double toroidal$getX();

    @Mutable
    @Accessor("x")
    void toroidal$setX(double x);

    @Accessor("z")
    double toroidal$getZ();

    @Mutable
    @Accessor("z")
    void toroidal$setZ(double z);
}
