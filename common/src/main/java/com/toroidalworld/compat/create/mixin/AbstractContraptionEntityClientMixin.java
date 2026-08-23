package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.create.CreateContraptionFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.contraptions.AbstractContraptionEntity", remap = false)
public abstract class AbstractContraptionEntityClientMixin {
    @WrapOperation(method = "handleStallPacket",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;"
                            + "handleStallInformation(DDDF)V"))
    private static void toroidal$stallInTheContraptionFrame(AbstractContraptionEntityClientMixin contraption,
            double x, double y, double z, float angle, Operation<Void> original) {
        Entity entity = (Entity) (Object) contraption;
        Vec3 folded = CreateContraptionFold.inFrameOf(entity, new Vec3(x, y, z));
        original.call(contraption, folded.x, folded.y, folded.z, angle);
    }
}
