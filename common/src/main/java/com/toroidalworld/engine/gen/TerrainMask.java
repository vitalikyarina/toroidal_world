package com.toroidalworld.engine.gen;

import java.util.BitSet;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.gen.TerrainSnapshot;
import com.toroidalworld.core.CoordinateConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class TerrainMask implements TerrainSnapshot {
    private final ChunkPos pos;

    private final int minY;

    private final CellGrid grid;

    private final BitSet solid;

    private final int lowestY;

    private final int highestY;

    private @Nullable BitSet written;

    private TerrainMask(ChunkPos pos, int minY, CellGrid grid, BitSet solid, int lowestY, int highestY) {
        this.pos = pos;
        this.minY = minY;
        this.grid = grid;
        this.solid = solid;
        this.lowestY = lowestY;
        this.highestY = highestY;
    }

    static TerrainMask of(boolean[] solid, ChunkPos pos, int minY, CellGrid grid) {
        BitSet bits = new BitSet(grid.cells());
        int lowestY = Integer.MAX_VALUE;
        int highestY = Integer.MIN_VALUE;

        for (int cell = 0; cell < solid.length; cell++) {
            if (!solid[cell]) {
                continue;
            }

            bits.set(cell);
            int y = minY + grid.layer(cell);
            lowestY = Math.min(lowestY, y);
            highestY = Math.max(highestY, y);
        }

        return new TerrainMask(pos, minY, grid, bits, lowestY, highestY);
    }

    public void wrote(BlockPos pos) {
        int cell = this.cellOf(pos.getX() & (CoordinateConstants.CHUNK_WIDTH - 1), pos.getY(),
                pos.getZ() & (CoordinateConstants.CHUNK_WIDTH - 1));
        if (cell == CellGrid.NO_CELL) {
            return;
        }

        if (this.written == null) {
            this.written = new BitSet(this.grid.cells());
        }

        this.written.set(cell);
    }

    ChunkPos pos() {
        return this.pos;
    }

    boolean isEmpty() {
        return this.lowestY > this.highestY;
    }

    int lowestY() {
        return this.lowestY;
    }

    int highestY() {
        return this.highestY;
    }

    boolean solidAt(int localX, int y, int localZ) {
        int cell = this.cellOf(localX, y, localZ);
        return cell != CellGrid.NO_CELL && this.solid.get(cell);
    }

    boolean untouchedAt(int localX, int y, int localZ) {
        int cell = this.cellOf(localX, y, localZ);
        if (cell == CellGrid.NO_CELL || !this.solid.get(cell)) {
            return false;
        }

        BitSet overwritten = this.written;
        return overwritten == null || !overwritten.get(cell);
    }

    int cellOf(int localX, int y, int localZ) {
        int layer = y - this.minY;
        if (layer < 0 || layer >= this.grid.layers()) {
            return CellGrid.NO_CELL;
        }

        return this.grid.cell(localX, layer, localZ);
    }
}
