package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = ToolboxHandlerClient.class, remap = false)
public class ToolboxHandlerClientMixin {
    @Unique
    private static final String STORED_POSITION = "Lnet/createmod/catnip/nbt/NBTHelper;"
            + "readBlockPos(Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;";

    @ModifyExpressionValue(method = "onKeyInput", at = @At(value = "INVOKE", target = STORED_POSITION))
    private static BlockPos toroidal$foldMenuToolbox(BlockPos canonical) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, canonical);
    }

    @ModifyExpressionValue(method = "renderOverlay", at = @At(value = "INVOKE", target = STORED_POSITION))
    private static BlockPos toroidal$foldHudToolbox(BlockPos canonical) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, canonical);
    }
}
