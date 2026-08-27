package com.toroidalworld.compat.sable.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.sable.client.SableClientFrame;

import dev.ryanhcode.sable.network.packets.ClientboundSableSnapshotDualPacket;
import dev.ryanhcode.sable.network.packets.PacketReceiveMode;

import net.minecraft.world.level.Level;

@Mixin(ClientboundSableSnapshotDualPacket.class)
public class ClientboundSableSnapshotDualPacketMixin {
    @Shadow
    @Final
    private List<ClientboundSableSnapshotDualPacket.Entry> entries;

    @Inject(
            method = "handleClient(Lnet/minecraft/world/level/Level;Ldev/ryanhcode/sable/network/packets/PacketReceiveMode;)V",
            at = @At("HEAD"))
    private void toroidal$reseatReceivedPoses(Level level, PacketReceiveMode mode, CallbackInfo ci) {
        for (ClientboundSableSnapshotDualPacket.Entry entry : this.entries) {
            SableClientFrame.reseat(level, entry.pose());
        }
    }
}
