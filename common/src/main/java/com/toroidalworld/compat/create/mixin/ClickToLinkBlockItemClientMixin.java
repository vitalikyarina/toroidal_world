package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
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

    @Inject(method = "clientTick", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;equals(Ljava/lang/Object;)Z"))
    private static void toroidal$skipOutlineWhileCopyUnheld(CallbackInfo callback, @Local BlockPos selected) {
        if (CreateClientFrame.heldCopy(Minecraft.getInstance().level, selected) == null) {
            callback.cancel();
        }
    }
}
