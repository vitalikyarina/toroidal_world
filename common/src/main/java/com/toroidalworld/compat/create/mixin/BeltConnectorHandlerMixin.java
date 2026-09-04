package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorHandler;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(value = BeltConnectorHandler.class, remap = false)
public class BeltConnectorHandlerMixin {
    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static Object toroidal$foldStoredPulley(Object stored) {
        if (!(stored instanceof BlockPos storedPulley)) {
            return stored;
        }

        return CreateClientFrame.inViewerFrame(storedPulley);
    }
}
