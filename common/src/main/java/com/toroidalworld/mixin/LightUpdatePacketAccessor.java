package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;

// Same in-place header swap as LevelChunkPacketAccessor. Vanilla shares one light packet across all border players
// (ChunkHolder.broadcastChanges), so mutating is only safe because ChunkHolderMixin splits that broadcast into a
// packet per player on wrapped levels.
@Mixin(ClientboundLightUpdatePacket.class)
public interface LightUpdatePacketAccessor {
    @Mutable
    @Accessor("x")
    void toroidal$setX(int x);

    @Mutable
    @Accessor("z")
    void toroidal$setZ(int z);
}
