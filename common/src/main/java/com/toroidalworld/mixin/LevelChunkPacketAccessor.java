package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

@Mixin(ClientboundLevelChunkWithLightPacket.class)
public interface LevelChunkPacketAccessor {
    @Mutable
    @Accessor("x")
    void toroidal$setX(int x);

    @Mutable
    @Accessor("z")
    void toroidal$setZ(int z);
}
