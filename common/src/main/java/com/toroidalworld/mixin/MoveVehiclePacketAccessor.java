package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;

// The vanilla wire pair behind the packet's STREAM_CODEC — the translator re-encodes through these instead of the
// codec field, which mods are free to wrap (see PacketTranslator.rewritePosition).
@Mixin(ClientboundMoveVehiclePacket.class)
public interface MoveVehiclePacketAccessor {
    @Invoker("write")
    void toroidal$write(FriendlyByteBuf output);

    @Invoker("<init>")
    static ClientboundMoveVehiclePacket toroidal$create(FriendlyByteBuf input) {
        throw new AssertionError();
    }
}
