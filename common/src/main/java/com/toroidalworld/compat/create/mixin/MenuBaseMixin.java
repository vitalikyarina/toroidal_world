package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import com.toroidalworld.compat.create.client.CreateMenuFrame;

import net.minecraft.network.RegistryFriendlyByteBuf;

@Mixin(value = MenuBase.class, remap = false)
public class MenuBaseMixin {
    @WrapOperation(
            method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/RegistryFriendlyByteBuf;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/gui/menu/MenuBase;createOnClient(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Ljava/lang/Object;"))
    private Object toroidal$readPayloadInClientFrame(MenuBase<?> menu, RegistryFriendlyByteBuf extraData,
            Operation<Object> original) {
        return CreateMenuFrame.readingPayload(() -> original.call(menu, extraData));
    }
}
