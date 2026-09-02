package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.simibubi.create.compat.trainmap.TrainMapRenderer;
import com.toroidalworld.compat.create.client.TrainMapViewFold;

import net.minecraft.core.BlockPos;

@Mixin(value = TrainMapRenderer.class, remap = false)
public abstract class TrainMapRendererMixin {
    @WrapMethod(method = "setPixel")
    private void toroidal$setFoldedPixel(int xCoord, int zCoord, int color, Operation<Void> original) {
        long pixel = TrainMapViewFold.foldPixelNode(xCoord, zCoord);
        original.call(BlockPos.getX(pixel), BlockPos.getZ(pixel), color);
    }

    @WrapMethod(method = "getPixel")
    private int toroidal$getFoldedPixel(int xCoord, int zCoord, Operation<Integer> original) {
        long pixel = TrainMapViewFold.foldPixelNode(xCoord, zCoord);
        return original.call(BlockPos.getX(pixel), BlockPos.getZ(pixel));
    }

    @WrapMethod(method = "blendPixel")
    private void toroidal$blendFoldedPixel(int xCoord, int zCoord, int color, int alpha, Operation<Void> original) {
        long pixel = TrainMapViewFold.foldPixelNode(xCoord, zCoord);
        original.call(BlockPos.getX(pixel), BlockPos.getZ(pixel), color, alpha);
    }
}
