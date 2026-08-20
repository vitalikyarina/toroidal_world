package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

@Mixin(value = TrackTargetingClient.class, remap = false)
public class TrackTargetingClientMixin {
    @WrapOperation(method = "clientTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static Object toroidal$foldHoveredTrack(ItemStack stack, DataComponentType<?> component,
            Operation<Object> original) {
        Object value = original.call(stack, component);
        if (component != AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS
                || !(value instanceof BlockPos selected)) {
            return value;
        }

        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, selected);
    }
}
