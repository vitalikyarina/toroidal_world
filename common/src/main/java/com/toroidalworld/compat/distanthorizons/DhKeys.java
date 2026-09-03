package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.ToroidalShape;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import com.seibel.distanthorizons.core.sql.dto.ChunkHashDTO;
import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.sql.dto.IBaseDTO;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

public final class DhKeys {
    public static long foldSection(ToroidalShape shape, long pos) {
        byte detail = DhSectionPos.getDetailLevel(pos);
        int rawX = DhSectionPos.getX(pos);
        int rawZ = DhSectionPos.getZ(pos);
        int x = DhFold.foldSection(shape, Direction.Axis.X, detail, rawX);
        int z = DhFold.foldSection(shape, Direction.Axis.Z, detail, rawZ);
        if (x == rawX && z == rawZ) {
            return pos;
        }

        return DhSectionPos.encode(detail, x, z);
    }

    public static DhChunkPos foldChunk(ToroidalShape shape, DhChunkPos pos) {
        int x = shape.foldChunk(Direction.Axis.X, pos.getX());
        int z = shape.foldChunk(Direction.Axis.Z, pos.getZ());
        if (x == pos.getX() && z == pos.getZ()) {
            return pos;
        }

        return new DhChunkPos(x, z);
    }

    public static long nearestSection(ToroidalShape shape, int refBlockX, int refBlockZ, long pos) {
        byte detail = DhSectionPos.getDetailLevel(pos);
        int rawX = DhSectionPos.getX(pos);
        int rawZ = DhSectionPos.getZ(pos);
        int x = DhFold.nearestSection(shape, Direction.Axis.X, detail, refBlockX, rawX);
        int z = DhFold.nearestSection(shape, Direction.Axis.Z, detail, refBlockZ, rawZ);
        if (x == rawX && z == rawZ) {
            return pos;
        }

        return DhSectionPos.encode(detail, x, z);
    }

    public static ChunkPos foldChunk(ToroidalShape shape, ChunkPos pos) {
        int x = shape.foldChunk(Direction.Axis.X, pos.x);
        int z = shape.foldChunk(Direction.Axis.Z, pos.z);
        if (x == pos.x && z == pos.z) {
            return pos;
        }

        return new ChunkPos(x, z);
    }

    public static DhBlockPos foldBlock(ToroidalShape shape, DhBlockPos pos) {
        int x = shape.foldBlock(Direction.Axis.X, pos.getX());
        int z = shape.foldBlock(Direction.Axis.Z, pos.getZ());
        if (x == pos.getX() && z == pos.getZ()) {
            return pos;
        }

        return new DhBlockPos(x, pos.getY(), z);
    }

    public static void reseat(IBaseDTO<?> dto, Object key) {
        if (dto instanceof FullDataSourceV2DTO section && key instanceof Long pos) {
            section.pos = pos;
        } else if (dto instanceof ChunkHashDTO chunk && key instanceof DhChunkPos pos) {
            chunk.pos = pos;
        } else if (dto instanceof BeaconBeamDTO beam && key instanceof DhBlockPos pos) {
            beam.blockPos = pos;
        }
    }

    private DhKeys() {
    }
}
