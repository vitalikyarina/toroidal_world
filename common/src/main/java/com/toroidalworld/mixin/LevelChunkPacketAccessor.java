package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.toroidalworld.accessors.ChunkPacketPosition;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

@Mixin(ClientboundLevelChunkWithLightPacket.class)
public interface LevelChunkPacketAccessor extends ChunkPacketPosition {
    @Mutable
    @Accessor("x")
    void toroidal$setX(int x);

    @Mutable
    @Accessor("z")
    void toroidal$setZ(int z);
}
