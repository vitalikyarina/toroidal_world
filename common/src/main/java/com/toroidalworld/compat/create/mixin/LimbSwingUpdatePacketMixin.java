package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.sync.LimbSwingUpdatePacket;
import com.toroidalworld.compat.create.CreateContraptionFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = LimbSwingUpdatePacket.class, remap = false)
public abstract class LimbSwingUpdatePacketMixin {
    @WrapOperation(method = "handle",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpTo(DDDFFI)V"))
    private void toroidal$lerpInTheRiderFrame(Entity rider, double x, double y, double z, float yRot, float xRot,
            int steps, Operation<Void> original) {
        Vec3 folded = CreateContraptionFold.inFrameOf(rider, new Vec3(x, y, z));
        original.call(rider, folded.x, folded.y, folded.z, yRot, xRot, steps);
    }
}
