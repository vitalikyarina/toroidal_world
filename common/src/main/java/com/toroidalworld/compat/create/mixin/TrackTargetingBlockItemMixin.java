package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

@Mixin(value = TrackTargetingBlockItem.class, remap = false)
public class TrackTargetingBlockItemMixin {
    @WrapOperation(method = "useOn",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object toroidal$foldSelectedTrack(ItemStack stack, DataComponentType<?> component,
            Operation<Object> original, UseOnContext context) {
        Object value = original.call(stack, component);
        if (component != AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS
                || !(value instanceof BlockPos selected)) {
            return value;
        }

        return CreateSeamFold.nearestCopy(context.getLevel(), context.getClickedPos(), selected);
    }
}
