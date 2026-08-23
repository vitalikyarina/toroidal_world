package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.equipment.zapper.ZapperBeamPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.world.phys.Vec3;

@Mixin(value = ZapperBeamPacket.class, remap = false)
public abstract class ZapperBeamPacketMixin {
    @Mutable
    @Shadow
    private Vec3 target;

    @Inject(method = "handleAdditional", at = @At("HEAD"))
    private void toroidal$beamEndInTheViewerFrame(CallbackInfo callback) {
        target = CreateClientFrame.nearestCopy(CreateClientFrame.camera(), target);
    }
}
