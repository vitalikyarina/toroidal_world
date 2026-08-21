package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.trains.track.CurvedTrackDestroyPacket;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

@Mixin(value = CurvedTrackDestroyPacket.class, remap = false)
public class CurvedTrackDestroyPacketMixin {
    @ModifyExpressionValue(
            method = "applySettings",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/trains/track/CurvedTrackDestroyPacket;targetPos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$canonicaliseTargetPos(BlockPos targetPos, ServerPlayer player, TrackBlockEntity be) {
        return CreateSeamFold.canonical(be.getLevel(), targetPos);
    }

    @ModifyExpressionValue(
            method = "applySettings",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/trains/track/CurvedTrackDestroyPacket;soundSource:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$canonicaliseSoundSource(BlockPos soundSource, ServerPlayer player, TrackBlockEntity be) {
        return CreateSeamFold.canonical(be.getLevel(), soundSource);
    }
}
