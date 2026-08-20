package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;

@Mixin(value = BeltConnectorItem.class, remap = false)
public class BeltConnectorItemMixin {
    @ModifyExpressionValue(
            method = "useOn",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object toroidal$foldStoredPulley(Object stored, UseOnContext context) {
        if (!(stored instanceof BlockPos storedPulley)) {
            return stored;
        }

        return CreateSeamFold.foldPosition(context.getLevel(), context.getClickedPos(), storedPulley);
    }
}
