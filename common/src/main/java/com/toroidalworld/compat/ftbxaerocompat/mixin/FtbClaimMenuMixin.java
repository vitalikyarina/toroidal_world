package com.toroidalworld.compat.ftbxaerocompat.mixin;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;
import com.toroidalworld.compat.ftbchunks.FtbChunksInjectionTargets;
import com.toroidalworld.compat.ftbxaerocompat.FtbXaeroFold;

import net.minecraft.world.level.ChunkPos;

import dev.ftb.mods.ftblibrary.math.XZ;

import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

@Mixin(targets = "dev.satherov.ftbxaerocompat.FTBClaimMenu", remap = false)
public abstract class FtbClaimMenuMixin {
    @ModifyExpressionValue(method = "addRightClickOptions",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private static ChunkPos toroidal$anchorPlayerToSelection(ChunkPos player, GuiMap screen,
            ArrayList<RightClickOption> options, MapTileSelection selection, MapProcessor processor) {
        return FtbXaeroFold.playerChunkNearSelection(player, selection);
    }

    @WrapOperation(method = "addRightClickOptions",
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_OF))
    private static XZ toroidal$foldSelectedChunk(int chunkX, int chunkZ, Operation<XZ> original) {
        return FtbChunksFold.chunkOf(chunkX, chunkZ);
    }
}
