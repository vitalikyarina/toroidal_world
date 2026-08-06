package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

// The chunk blob and light blob are opaque and position-independent — only the two header ints name the chunk. The
// packet is built fresh for every recipient (PlayerChunkSender.sendChunk), so the header can be swapped in place
// instead of re-encoding the whole packet.
@Mixin(ClientboundLevelChunkWithLightPacket.class)
public interface LevelChunkPacketAccessor {
    @Mutable
    @Accessor("x")
    void toroidal$setX(int x);

    @Mutable
    @Accessor("z")
    void toroidal$setZ(int z);
}
