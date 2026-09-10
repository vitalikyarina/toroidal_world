package com.toroidalworld.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;

import net.minecraft.core.Direction;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;

@Mixin(value = FTBChunksClient.class, remap = false)
public abstract class FTBChunksClientMixin {
    private static final String UPDATE_MINIMAP =
            "updateMinimapIfNeeded(IILdev/ftb/mods/ftbchunks/client/map/MapDimension;Z)V";

    private static final String REGION_SPLITS =
            "Ldev/ftb/mods/ftbchunks/client/FTBChunksClient;regionSplits(I)[I";

    private static final String MAP_ICON_ENTITY =
            "lambda$mapIcons$14(Lnet/minecraft/world/entity/Entity;"
                    + "Ldev/ftb/mods/ftbchunks/api/client/event/MapIconEvent;"
                    + "Ldev/ftb/mods/ftblibrary/icon/Icon;"
                    + "Ldev/ftb/mods/ftblibrary/icon/EntityIconLoader$WidthHeight;"
                    + "Ldev/ftb/mods/ftbchunks/client/map/MapDimension;)V";

    @WrapOperation(method = UPDATE_MINIMAP, at = @At(value = "INVOKE", target = REGION_SPLITS, ordinal = 0))
    private int[] toroidal$splitMinimapAlongX(int centreChunk, Operation<int[]> original) {
        return FtbChunksFold.minimapSplits(Direction.Axis.X, centreChunk, original.call(centreChunk));
    }

    @WrapOperation(method = UPDATE_MINIMAP, at = @At(value = "INVOKE", target = REGION_SPLITS, ordinal = 1))
    private int[] toroidal$splitMinimapAlongZ(int centreChunk, Operation<int[]> original) {
        return FtbChunksFold.minimapSplits(Direction.Axis.Z, centreChunk, original.call(centreChunk));
    }

    @ModifyVariable(method = UPDATE_MINIMAP, at = @At("STORE"), name = "ox")
    private int toroidal$foldMinimapPieceX(int chunkX) {
        return FtbChunksFold.foldChunk(Direction.Axis.X, chunkX);
    }

    @ModifyVariable(method = UPDATE_MINIMAP, at = @At("STORE"), name = "oz")
    private int toroidal$foldMinimapPieceZ(int chunkZ) {
        return FtbChunksFold.foldChunk(Direction.Axis.Z, chunkZ);
    }

    @ModifyVariable(method = MAP_ICON_ENTITY, at = @At("STORE"), name = "x")
    private static int toroidal$foldIconBlockX(int x) {
        return FtbChunksFold.foldBlock(Direction.Axis.X, x);
    }

    @ModifyVariable(method = MAP_ICON_ENTITY, at = @At("STORE"), name = "z")
    private static int toroidal$foldIconBlockZ(int z) {
        return FtbChunksFold.foldBlock(Direction.Axis.Z, z);
    }
}
