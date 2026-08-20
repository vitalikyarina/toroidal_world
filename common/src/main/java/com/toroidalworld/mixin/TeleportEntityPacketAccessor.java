package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;

@Mixin(ClientboundTeleportEntityPacket.class)
public interface TeleportEntityPacketAccessor {
    @Invoker("write")
    void toroidal$write(FriendlyByteBuf output);

    @Invoker("<init>")
    static ClientboundTeleportEntityPacket toroidal$create(FriendlyByteBuf input) {
        throw new AssertionError();
    }
}
