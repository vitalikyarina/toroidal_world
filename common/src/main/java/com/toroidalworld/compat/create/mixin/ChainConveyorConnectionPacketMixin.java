package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionPacket;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

@Mixin(value = ChainConveyorConnectionPacket.class, remap = false)
public class ChainConveyorConnectionPacketMixin {
    @ModifyExpressionValue(
            method = "applySettings",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorConnectionPacket;targetPos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldTargetPos(BlockPos targetPos, ServerPlayer player, ChainConveyorBlockEntity be) {
        return CreateSeamFold.nearestCopy(be.getLevel(), be.getBlockPos(), targetPos);
    }
}
