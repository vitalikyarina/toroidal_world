package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

@Mixin(value = BlockEntityDataPacket.class, remap = false)
public class BlockEntityDataPacketMixin {
    @ModifyExpressionValue(
            method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/foundation/networking/BlockEntityDataPacket;pos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldLookupIntoTheClientFrame(BlockPos canonical, LocalPlayer player) {
        return CreateClientFrame.nearestCopy(player.clientLevel, canonical);
    }
}
