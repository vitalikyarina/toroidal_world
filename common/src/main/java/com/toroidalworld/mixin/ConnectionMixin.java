package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.net.PacketTranslator;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

// The one place the server's wrapped coordinates are turned into the unbounded ones the client believes in — and back.
@Mixin(Connection.class)
public class ConnectionMixin {
    @Shadow
    public @Nullable PacketListener getPacketListener() {
        throw new AssertionError();
    }

    @WrapMethod(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V")
    private void toroidal$translateOutgoing(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush,
            Operation<Void> original) {
        ServerPlayer player = toroidal$playerOf(this.getPacketListener());
        if (player == null) {
            original.call(packet, listener, flush);
            return;
        }

        // A null translation means the packet is deliberately dropped — a position correction for the rider's own
        // vehicle, which they predict locally and which would only make them jolt across the seam.
        Packet<?> translated = PacketTranslator.toClient(packet, player);
        if (translated != null) {
            original.call(translated, listener, flush);
        }
    }

    @ModifyVariable(method = "channelRead0", at = @At("HEAD"), argsOnly = true)
    private Packet<?> toroidal$translateIncoming(Packet<?> packet) {
        ServerPlayer player = toroidal$playerOf(this.getPacketListener());
        return player == null ? packet : PacketTranslator.toServer(packet, player);
    }

    @Unique
    private static @Nullable ServerPlayer toroidal$playerOf(@Nullable PacketListener listener) {
        return listener instanceof ServerGamePacketListenerImpl gameListener ? gameListener.player : null;
    }
}
