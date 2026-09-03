package com.toroidalworld.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.distanthorizons.DhKeys;
import com.toroidalworld.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

@Mixin(ChunkWrapper.class)
public class ChunkWrapperMixin {
    @Shadow
    @Final
    private ILevelWrapper wrappedLevel;

    @Unique
    private ChunkPos toroidal$foldedPos;

    @WrapOperation(
            method = {
                "<init>(Lnet/minecraft/world/level/chunk/ChunkAccess;"
                        + "Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper;)V",
                "getMinBlockX", "getMinBlockZ", "getMaxBlockX", "getMaxBlockZ"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getPos()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos toroidal$foldedChunkPos(ChunkAccess chunk, Operation<ChunkPos> original) {
        ChunkPos folded = this.toroidal$foldedPos;
        if (folded == null) {
            ChunkPos raw = original.call(chunk);
            ToroidalShape shape = DhShapes.of(this.wrappedLevel);
            folded = shape == null ? raw : DhKeys.foldChunk(shape, raw);
            this.toroidal$foldedPos = folded;
        }

        return folded;
    }
}
