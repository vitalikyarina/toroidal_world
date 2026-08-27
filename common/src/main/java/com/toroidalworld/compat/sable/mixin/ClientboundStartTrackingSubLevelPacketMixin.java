package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.sable.client.SableClientFrame;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStartTrackingSubLevelPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

@Mixin(value = ClientboundStartTrackingSubLevelPacket.class, remap = false)
public class ClientboundStartTrackingSubLevelPacketMixin {
    @Shadow
    @Final
    private Pose3dc lastPose;

    @Shadow
    @Final
    private Pose3d pose;

    @Inject(method = "handle", at = @At("HEAD"))
    private void toroidal$reseatReceivedPoses(CallbackInfo ci) {
        Level level = Minecraft.getInstance().level;
        SableClientFrame.reseat(level, this.lastPose);
        SableClientFrame.reseat(level, this.pose);
    }
}
