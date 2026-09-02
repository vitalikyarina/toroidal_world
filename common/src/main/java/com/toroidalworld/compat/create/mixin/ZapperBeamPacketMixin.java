package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.simibubi.create.content.equipment.zapper.ZapperBeamPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.world.phys.Vec3;

@Mixin(value = ZapperBeamPacket.class, remap = false)
public abstract class ZapperBeamPacketMixin {
    @ModifyArgs(
            method = "handleAdditional",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/equipment/zapper/ZapperRenderHandler$LaserBeam;"
                            + "<init>(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"))
    private void toroidal$beamEndOnTheFoldedMuzzle(Args args) {
        Vec3 muzzle = args.get(0);
        Vec3 raw = args.get(1);
        args.set(1, CreateClientFrame.nearestCopy(muzzle, raw));
    }
}
