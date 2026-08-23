package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.equipment.zapper.ShootGadgetPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ShootGadgetPacket.class, remap = false)
public abstract class ShootGadgetPacketMixin {
    @Mutable
    @Shadow
    protected Vec3 location;

    @Inject(method = "handle", at = @At("HEAD"))
    private void toroidal$muzzleInTheViewerFrame(LocalPlayer player, CallbackInfo callback) {
        location = CreateClientFrame.nearestCopy(CreateClientFrame.camera(), location);
    }
}
