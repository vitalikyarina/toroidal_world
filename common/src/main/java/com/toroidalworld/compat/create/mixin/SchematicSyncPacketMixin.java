package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.schematics.packet.SchematicSyncPacket;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

@Mixin(value = SchematicSyncPacket.class, remap = false)
public class SchematicSyncPacketMixin {
    @ModifyExpressionValue(
            method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/schematics/packet/SchematicSyncPacket;"
                            + "anchor:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$canonicaliseAnchor(BlockPos anchor, @Local(argsOnly = true) ServerPlayer player) {
        return player == null ? anchor : CreateSeamFold.canonical(player.level(), anchor);
    }
}
