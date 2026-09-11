package com.toroidalworld.compat.ftbxaerocompat;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.ftbchunks.FtbChunksFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.math.XZ;

import xaero.map.gui.MapTileSelection;

public final class FtbXaeroFold {
    public static ChunkPos playerChunkNearSelection(ChunkPos player, @Nullable MapTileSelection selection) {
        if (selection == null) {
            return player;
        }

        int midX = Math.floorDiv(selection.getLeft() + selection.getRight(), 2);
        int midZ = Math.floorDiv(selection.getTop() + selection.getBottom(), 2);
        return new ChunkPos(
                FtbChunksFold.nearestChunk(Direction.Axis.X, midX, player.x),
                FtbChunksFold.nearestChunk(Direction.Axis.Z, midZ, player.z));
    }

    public static boolean regionHasStoredClaims(int regionX, int regionZ) {
        MapDimension dimension = MapDimension.getCurrent().orElse(null);
        if (dimension == null) {
            return false;
        }

        Map<XZ, MapRegion> regions = dimension.getRegions();
        int[] canonicalX = FtbChunksFold.canonicalRegions(Direction.Axis.X, regionX);
        int[] canonicalZ = FtbChunksFold.canonicalRegions(Direction.Axis.Z, regionZ);
        for (int x : canonicalX) {
            for (int z : canonicalZ) {
                if (regions.get(XZ.of(x, z)) != null) {
                    return true;
                }
            }
        }

        return false;
    }

    private FtbXaeroFold() {
    }
}
