package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.client.engine.ClientFrame;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin {
    @Shadow
    @Final
    private ClientLevel level;

    @Shadow
    @Final
    private LevelChunk emptyChunk;

    @WrapMethod(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)"
                    + "Lnet/minecraft/world/level/chunk/LevelChunk;")
    private @Nullable LevelChunk toroidal$heldChunk(int chunkX, int chunkZ, ChunkStatus status, boolean loadOrGenerate,
            Operation<@Nullable LevelChunk> original) {
        LevelChunk chunk = original.call(chunkX, chunkZ, status, loadOrGenerate);
        if (chunk != null && chunk != this.emptyChunk) {
            return chunk;
        }

        if (this.level != Minecraft.getInstance().level) {
            return chunk;
        }

        ChunkPos raw = new ChunkPos(chunkX, chunkZ);
        ChunkPos held = ClientFrame.heldCopy(raw, pos -> original.call(pos.x, pos.z, status, false) != null);
        if (held == null || held.equals(raw)) {
            return chunk;
        }

        return original.call(held.x, held.z, status, loadOrGenerate);
    }
}
