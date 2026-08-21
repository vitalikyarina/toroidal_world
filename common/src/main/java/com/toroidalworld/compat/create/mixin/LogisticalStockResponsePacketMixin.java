package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.stockTicker.LogisticalStockResponsePacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

@Mixin(value = LogisticalStockResponsePacket.class, remap = false)
public class LogisticalStockResponsePacketMixin {
    @ModifyExpressionValue(
            method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/logistics/stockTicker/LogisticalStockResponsePacket;pos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldTickerIntoTheClientFrame(BlockPos canonical, LocalPlayer player) {
        return CreateClientFrame.nearestCopy(player.clientLevel, canonical);
    }
}
