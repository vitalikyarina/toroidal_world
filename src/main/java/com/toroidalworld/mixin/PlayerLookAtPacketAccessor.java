package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;

// The look-at target hides in private final fields with no rebuild path — the at-entity constructor demands a live
// Entity the translator does not have. Vanilla creates a fresh instance for every send (ServerPlayer.lookAt), so the
// coordinate can be swapped in place.
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
