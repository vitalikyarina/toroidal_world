package com.toroidalworld.engine.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.gen.TerrainSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class TerrainMask implements TerrainSnapshot {
    private static final int CHUNK_COLUMNS = 16;

    private static final int COLUMN_CELLS = CHUNK_COLUMNS * CHUNK_COLUMNS;

    private static final int NO_CELL = -1;

    private final ChunkPos pos;

    private final int minY;

    private final int height;

    private final long[] solid;

    private final int lowestY;

    private final int highestY;

    private long @Nullable [] written;

    private TerrainMask(ChunkPos pos, int minY, int height, long[] solid, int lowestY, int highestY) {
        this.pos = pos;
        this.minY = minY;
        this.height = height;
        this.solid = solid;
        this.lowestY = lowestY;
        this.highestY = highestY;
    }

    static TerrainMask of(boolean[] solid, ChunkPos pos, int minY, int height) {
        long[] bits = new long[(solid.length + Long.SIZE - 1) / Long.SIZE];
        int lowestY = Integer.MAX_VALUE;
        int highestY = Integer.MIN_VALUE;

        for (int cell = 0; cell < solid.length; cell++) {
            if (!solid[cell]) {
                continue;
            }

            bits[cell / Long.SIZE] |= 1L << (cell % Long.SIZE);
            int y = minY + cell / COLUMN_CELLS;
            lowestY = Math.min(lowestY, y);
            highestY = Math.max(highestY, y);
        }

        return new TerrainMask(pos, minY, height, bits, lowestY, highestY);
    }

    public void wrote(BlockPos pos) {
        int cell = this.cellOf(pos.getX() & (CHUNK_COLUMNS - 1), pos.getY(), pos.getZ() & (CHUNK_COLUMNS - 1));
        if (cell == NO_CELL) {
            return;
        }

        if (this.written == null) {
            this.written = new long[this.solid.length];
        }

        this.written[cell / Long.SIZE] |= 1L << (cell % Long.SIZE);
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
        return cell != NO_CELL && (this.solid[cell / Long.SIZE] & 1L << (cell % Long.SIZE)) != 0L;
    }

    boolean untouchedAt(int localX, int y, int localZ) {
        int cell = this.cellOf(localX, y, localZ);
        if (cell == NO_CELL || (this.solid[cell / Long.SIZE] & 1L << (cell % Long.SIZE)) == 0L) {
            return false;
        }

        long[] overwritten = this.written;
        return overwritten == null || (overwritten[cell / Long.SIZE] & 1L << (cell % Long.SIZE)) == 0L;
    }

    int cellOf(int localX, int y, int localZ) {
        if (y < this.minY || y >= this.minY + this.height) {
            return NO_CELL;
        }

        return localX + localZ * CHUNK_COLUMNS + (y - this.minY) * COLUMN_CELLS;
    }
}
