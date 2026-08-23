package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.redstone.displayLink.ClickToLinkBlockItem;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = ClickToLinkBlockItem.class, remap = false)
public class ClickToLinkBlockItemClientMixin {
    @ModifyExpressionValue(method = "clientTick",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/redstone/displayLink/ClickToLinkBlockItem$ClickToLinkData;"
                            + "selectedPos()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$foldOutlineToClientFrame(BlockPos selected) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, selected);
    }
}
