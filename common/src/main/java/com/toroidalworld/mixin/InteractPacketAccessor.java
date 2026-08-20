package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;

@Mixin(ServerboundInteractPacket.class)
public interface InteractPacketAccessor {
    @Invoker("write")
    void toroidal$write(FriendlyByteBuf output);

    @Invoker("<init>")
    static ServerboundInteractPacket toroidal$create(FriendlyByteBuf input) {
        throw new AssertionError();
    }
}
