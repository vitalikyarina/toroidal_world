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
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

// On 26.x DH ships one ChunkWrapper; on this game line the game is obfuscated, so it ships one per loader, each
// naming the chunk type in that loader's own mapping. Each is unloadable on the other loader, so both are named here
// and only the running loader's copy is ever transformed. The constructor is named in both mappings for the same
// reason GuiMapMixin names its render override twice: with two targets the remapper resolves neither descriptor.
@Mixin(targets = {
        "com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper_neoforge",
        "com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper_fabric"})
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
                "<init>(Lnet/minecraft/class_2791;"
                        + "Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper;)V",
                "getMinBlockX", "getMinBlockZ", "getMaxBlockX", "getMaxBlockZ"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getPos()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos toroidal$foldedChunkPos(ChunkAccess chunk, Operation<ChunkPos> original) {
        ChunkPos folded = this.toroidal$foldedPos;
        if (folded == null) {
            ChunkPos raw = original.call(chunk);
            ToroidalShape shape = DhShapes.withFoldedKeys(DhShapes.of(this.wrappedLevel));
            folded = shape == null ? raw : DhKeys.foldChunk(shape, raw);
            this.toroidal$foldedPos = folded;
        }

        return folded;
    }
}
