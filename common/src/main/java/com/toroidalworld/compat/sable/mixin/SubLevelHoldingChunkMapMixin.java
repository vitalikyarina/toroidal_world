package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.compat.sable.SableChunkKeys;

import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Mixin(value = SubLevelHoldingChunkMap.class, remap = false)
public class SubLevelHoldingChunkMapMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @ModifyVariable(method = "moveToUnloaded", at = @At("HEAD"), argsOnly = true)
    private ChunkPos toroidal$holdInPhysicalChunk(ChunkPos pos) {
        return SableChunkKeys.physical(this.level, pos);
    }
}
