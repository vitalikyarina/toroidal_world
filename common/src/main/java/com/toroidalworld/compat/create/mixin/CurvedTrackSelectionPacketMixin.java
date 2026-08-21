package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.trains.track.CurvedTrackSelectionPacket;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

@Mixin(value = CurvedTrackSelectionPacket.class, remap = false)
public class CurvedTrackSelectionPacketMixin {
    @ModifyExpressionValue(
            method = "applySettings",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/trains/track/CurvedTrackSelectionPacket;targetPos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$canonicaliseTargetPos(BlockPos targetPos, ServerPlayer player, TrackBlockEntity be) {
        return CreateSeamFold.canonical(be.getLevel(), targetPos);
    }
}
