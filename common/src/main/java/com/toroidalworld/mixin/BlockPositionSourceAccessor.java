package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.gameevent.BlockPositionSource;

// The block a vibration is travelling to. The field is private and the only public reader takes a Level and hands back
// a Vec3 at the block's centre — a level the translator deliberately does not carry, and a round trip through floating
// point for a value that is already a block.
@Mixin(BlockPositionSource.class)
public interface BlockPositionSourceAccessor {
    @Accessor("pos")
    BlockPos toroidal$getPos();
}
