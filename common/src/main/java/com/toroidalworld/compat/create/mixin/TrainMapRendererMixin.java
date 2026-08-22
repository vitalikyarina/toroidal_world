package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.simibubi.create.compat.trainmap.TrainMapRenderer;
import com.toroidalworld.compat.create.client.TrainMapViewFold;

@Mixin(value = TrainMapRenderer.class, remap = false)
public abstract class TrainMapRendererMixin {
    @ModifyVariable(method = "setPixel", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int toroidal$wrapSetX(int xCoord) {
        return TrainMapViewFold.wrapPixelX(xCoord);
    }

    @ModifyVariable(method = "setPixel", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int toroidal$wrapSetZ(int zCoord) {
        return TrainMapViewFold.wrapPixelZ(zCoord);
    }

    @ModifyVariable(method = "getPixel", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int toroidal$wrapGetX(int xCoord) {
        return TrainMapViewFold.wrapPixelX(xCoord);
    }

    @ModifyVariable(method = "getPixel", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int toroidal$wrapGetZ(int zCoord) {
        return TrainMapViewFold.wrapPixelZ(zCoord);
    }

    @ModifyVariable(method = "blendPixel", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int toroidal$wrapBlendX(int xCoord) {
        return TrainMapViewFold.wrapPixelX(xCoord);
    }

    @ModifyVariable(method = "blendPixel", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int toroidal$wrapBlendZ(int zCoord) {
        return TrainMapViewFold.wrapPixelZ(zCoord);
    }
}
