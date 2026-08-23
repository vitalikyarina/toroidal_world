package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.create.CreateContraptionFold;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

@Mixin(targets = "com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity", remap = false)
public abstract class GantryContraptionEntityClientMixin {
    @Shadow
    Direction movementAxis;

    @ModifyExpressionValue(method = "handlePacket",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/gantry/GantryContraptionUpdatePacket;"
                            + "coord()D"))
    private static double toroidal$syncedCoordInTheGantryFrame(double canonicalCoord,
            @Local GantryContraptionEntityClientMixin gantry) {
        Entity entity = (Entity) (Object) gantry;
        return CreateContraptionFold.axisInFrameOf(entity, gantry.movementAxis.getAxis(), canonicalCoord);
    }
}
