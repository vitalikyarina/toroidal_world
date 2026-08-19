package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;

@Mixin(ClientboundSetBorderCenterPacket.class)
public interface SetBorderCenterPacketAccessor {
    @Invoker("write")
    void toroidal$write(FriendlyByteBuf output);

    @Invoker("<init>")
    static ClientboundSetBorderCenterPacket toroidal$create(FriendlyByteBuf input) {
        throw new AssertionError();
    }
}
