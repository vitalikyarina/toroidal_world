package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.TerrainMaskHolder;
import com.toroidalworld.engine.gen.TerrainMask;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin implements TerrainMaskHolder {
    @Unique
    private @Nullable TerrainMask toroidal$terrainMask;

    @Override
    public @Nullable TerrainMask toroidal$terrainMask() {
        return this.toroidal$terrainMask;
    }

    @Override
    public void toroidal$terrainMask(TerrainMask mask) {
        this.toroidal$terrainMask = mask;
    }

    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"))
    private void toroidal$markWritten(BlockPos pos, BlockState state, int flags,
            CallbackInfoReturnable<@Nullable BlockState> callback) {
        TerrainMask mask = this.toroidal$terrainMask;
        if (mask != null) {
            mask.wrote(pos);
        }
    }
}
