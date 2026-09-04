package com.toroidalworld.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.toroidalworld.compat.aeronautics.client.RopeAnchorFrame;

import dev.simulated_team.simulated.content.items.rope.RopeItem.ClientRopeItemHandler;

import net.minecraft.core.BlockPos;

@Mixin(value = ClientRopeItemHandler.class, remap = false)
public class ClientRopeItemHandlerMixin {
    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"),
            require = 1,
            allow = 1)
    private static Object toroidal$seatFirstAnchor(Object stored) {
        return stored instanceof BlockPos canonical ? RopeAnchorFrame.nearestCopy(canonical) : stored;
    }
}
