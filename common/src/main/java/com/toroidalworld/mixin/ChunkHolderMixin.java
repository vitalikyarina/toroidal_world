package com.toroidalworld.mixin;

import java.util.BitSet;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin {
    @Shadow
    @Final
    private LevelLightEngine lightEngine;

    @Shadow
    @Final
    private BitSet skyChangedLightSectionFilter;

    @Shadow
    @Final
    private BitSet blockChangedLightSectionFilter;

    @WrapOperation(
            method = "broadcastChanges",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkHolder;broadcast(Ljava/util/List;Lnet/minecraft/network/protocol/Packet;)V",
                    ordinal = 0))
    private void toroidal$perPlayerLightPacket(ChunkHolder holder, List<ServerPlayer> players, Packet<?> packet,
            Operation<Void> original, @Local(argsOnly = true) LevelChunk chunk) {
        if (players.size() <= 1 || !WorldLoopAttachments.transformerOf(chunk.getLevel()).isWrapped()) {
            original.call(holder, players, packet);
            return;
        }

        original.call(holder, List.of(players.getFirst()), packet);
        for (ServerPlayer player : players.subList(1, players.size())) {
            original.call(holder, List.of(player), new ClientboundLightUpdatePacket(
                    chunk.getPos(), this.lightEngine,
                    this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter));
        }
    }
}
