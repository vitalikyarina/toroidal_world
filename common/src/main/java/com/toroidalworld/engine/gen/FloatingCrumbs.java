package com.toroidalworld.engine.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.CrumbSweepCache;
import com.toroidalworld.api.v1.gen.TerrainSnapshot;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.noise.TerrainCeiling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;

public final class FloatingCrumbs {
    public static final int CRUMB_CEILING_BLOCKS = 32;

    private static final int CHUNK_COLUMNS = 16;

    private static final int SECTION_BLOCKS = 16;

    private static final int WINDOW_CHUNKS = 3;

    private static final int WINDOW_COLUMNS = WINDOW_CHUNKS * CHUNK_COLUMNS;

    private static final int WINDOW_CELLS = WINDOW_COLUMNS * WINDOW_COLUMNS;

    private static final int CENTRE_INDEX = WINDOW_CHUNKS + 1;

    private static final int CLEARED_SEED = 64;

    static final byte NO_FLUID = 0;

    private static final int NO_CELL = -1;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static final long[] NO_POSITIONS = new long[0];

    record Sweep(int detached, int swept, int blocks) {
    }

    private static final class Cleared {
        private final int lowestY;
        private int[] cells = new int[CLEARED_SEED];
        private int count;

        private Cleared(int lowestY) {
            this.lowestY = lowestY;
        }

        private void add(int cell) {
            if (this.count == this.cells.length) {
                this.cells = Arrays.copyOf(this.cells, this.cells.length * 2);
            }

            this.cells[this.count++] = cell;
        }
    }

    public static boolean installedOn(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return ShapedChunkGenerator.wrappedTransformerOf(generator) != null
                && generator instanceof NoiseBasedChunkGenerator noise
                && TerrainCeiling.ceiling(noise.generatorSettings().value()) != null;
    }

    private static boolean sweepsCrumbs(ServerLevel level) {
        return ((CrumbSweepCache) level).toroidal$sweepsCrumbs();
    }

    public static void sweep(ServerLevel level, ChunkAccess chunk) {
        if (!sweepsCrumbs(level)) {
            return;
        }

        int minY = chunk.getMinY();
        int height = chunk.getHeight();
        boolean[] solid = new boolean[CHUNK_COLUMNS * CHUNK_COLUMNS * height];
        byte[] fluid = new byte[solid.length];
        List<BlockState> fluids = new ArrayList<>();
        int solidBlocks = fill(chunk, solid, fluid, fluids, minY);
        if (solidBlocks > 0) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            clearCrumbs(solid, fluid, height, solidBlocks,
                    cell -> write(chunk, cursor, cell, minY, blockOf(fluids, fluid[cell])));
        }

        WorldLoopAttachments.attachTerrainMask(chunk, TerrainMask.of(solid, chunk.getPos(), minY, height));
    }

    public static void registerMask(ServerLevel level, ChunkAccess chunk) {
        TerrainMask mask = WorldLoopAttachments.terrainMaskOf(chunk);
        if (mask != null) {
            WorldLoopAttachments.terrainMasksOf(level).put(chunk.getPos(), mask);
        }
    }

    public static void sweepAcross(ServerLevel level, ChunkAccess chunk,
            StaticCache2D<GenerationChunkHolder> chunks) {
        if (!sweepsCrumbs(level)) {
            return;
        }

        TerrainMasks masks = WorldLoopAttachments.terrainMasksOf(level);
        ChunkPos centre = chunk.getPos();
        long[] keys = new long[WINDOW_CHUNKS * WINDOW_CHUNKS];
        TerrainMask[] window = new TerrainMask[keys.length];

        for (int stepZ = -1; stepZ <= 1; stepZ++) {
            for (int stepX = -1; stepX <= 1; stepX++) {
                int index = (stepZ + 1) * WINDOW_CHUNKS + stepX + 1;
                keys[index] = chunks.get(centre.x() + stepX, centre.z() + stepZ).getPos().pack();
                window[index] = masks.at(keys[index]);
            }
        }

        sweepWindow(chunk, window);
        for (long key : keys) {
            masks.consumed(key);
        }
    }

    public static void sweepWindow(ChunkAccess chunk, TerrainMask[] window) {
        Cleared cleared = crumbCells(window);
        if (cleared == null || cleared.count == 0) {
            return;
        }

        TerrainMask centre = window[CENTRE_INDEX];
        int[] cells = new int[cleared.count];
        for (int i = 0; i < cleared.count; i++) {
            int cell = cleared.cells[i];
            cells[i] = centre.cellOf(centreLocalX(cell), cleared.lowestY + cell / WINDOW_CELLS,
                    centreLocalZ(cell));
        }

        clearInChunk(chunk, cells, cleared.count);
    }

    public static @Nullable TerrainSnapshot snapshotOf(ChunkAccess chunk) {
        return WorldLoopAttachments.terrainMaskOf(chunk);
    }

    public static long[] crumbPositions(TerrainSnapshot[] window) {
        TerrainMask[] masks = new TerrainMask[window.length];
        for (int i = 0; i < window.length; i++) {
            masks[i] = window[i] instanceof TerrainMask mask ? mask : null;
        }

        Cleared cleared = crumbCells(masks);
        if (cleared == null) {
            return NO_POSITIONS;
        }

        ChunkPos pos = masks[CENTRE_INDEX].pos();
        long[] positions = new long[cleared.count];
        for (int i = 0; i < cleared.count; i++) {
            int cell = cleared.cells[i];
            positions[i] = BlockPos.asLong(pos.getMinBlockX() + centreLocalX(cell),
                    cleared.lowestY + cell / WINDOW_CELLS,
                    pos.getMinBlockZ() + centreLocalZ(cell));
        }

        return positions;
    }

    private static @Nullable Cleared crumbCells(TerrainMask[] window) {
        int lowestY = Integer.MAX_VALUE;
        int highestY = Integer.MIN_VALUE;
        for (TerrainMask mask : window) {
            if (mask == null) {
                return null;
            }

            if (!mask.isEmpty()) {
                lowestY = Math.min(lowestY, mask.lowestY());
                highestY = Math.max(highestY, mask.highestY());
            }
        }

        TerrainMask centre = window[CENTRE_INDEX];
        if (lowestY > highestY || centre.isEmpty()) {
            return null;
        }

        int windowHeight = highestY - lowestY + 1;
        int words = (WINDOW_CELLS * windowHeight + Long.SIZE - 1) / Long.SIZE;
        long[] taken = new long[words];
        long[] rejected = new long[words];
        int[] pending = new int[CRUMB_CEILING_BLOCKS];
        Cleared cleared = new Cleared(lowestY);

        for (int y = centre.lowestY(); y <= centre.highestY(); y++) {
            for (int localZ = 0; localZ < CHUNK_COLUMNS; localZ++) {
                for (int localX = 0; localX < CHUNK_COLUMNS; localX++) {
                    if (!centre.solidAt(localX, y, localZ)) {
                        continue;
                    }

                    int start = windowCell(CHUNK_COLUMNS + localX, y - lowestY, CHUNK_COLUMNS + localZ);
                    if (!get(taken, start)) {
                        takeCrumb(window, taken, rejected, pending, cleared, start, windowHeight);
                    }
                }
            }
        }

        return cleared;
    }

    private static int centreLocalX(int cell) {
        return cell % WINDOW_COLUMNS - CHUNK_COLUMNS;
    }

    private static int centreLocalZ(int cell) {
        return cell / WINDOW_COLUMNS % WINDOW_COLUMNS - CHUNK_COLUMNS;
    }

    private static void takeCrumb(TerrainMask[] window, long[] taken, long[] rejected, int[] pending,
            Cleared cleared, int start, int windowHeight) {
        set(taken, start);
        pending[0] = start;
        int head = 0;
        int tail = 1;
        boolean overCeiling = false;

        while (head < tail && !overCeiling) {
            int cell = pending[head++];
            if (onWindowSide(cell)) {
                overCeiling = true;
                break;
            }

            for (int axis = 0; axis < 3 && !overCeiling; axis++) {
                for (int step = -1; step <= 1; step += 2) {
                    int neighbour = windowNeighbour(cell, axis, step, windowHeight);
                    if (neighbour == NO_CELL || !windowSolid(window, neighbour, cleared.lowestY)) {
                        continue;
                    }

                    if (get(rejected, neighbour) || tail == pending.length) {
                        overCeiling = true;
                        break;
                    }

                    if (!get(taken, neighbour)) {
                        set(taken, neighbour);
                        pending[tail++] = neighbour;
                    }
                }
            }
        }

        if (overCeiling || tail >= CRUMB_CEILING_BLOCKS) {
            for (int i = 0; i < tail; i++) {
                set(rejected, pending[i]);
            }

            return;
        }

        TerrainMask centre = window[CENTRE_INDEX];
        for (int i = 0; i < tail; i++) {
            int cell = pending[i];
            if (chunkIndexOf(cell) == CENTRE_INDEX
                    && centre.untouchedAt(centreLocalX(cell), cleared.lowestY + cell / WINDOW_CELLS,
                            centreLocalZ(cell))) {
                cleared.add(cell);
            }
        }
    }

    private static void clearInChunk(ChunkAccess chunk, int[] cells, int count) {
        int minY = chunk.getMinY();
        int height = chunk.getHeight();
        boolean[] solid = new boolean[CHUNK_COLUMNS * CHUNK_COLUMNS * height];
        byte[] fluid = new byte[solid.length];
        List<BlockState> fluids = new ArrayList<>();
        fill(chunk, solid, fluid, fluids, minY);

        Arrays.sort(cells, 0, count);
        floodCleared(fluid, height, cells, count);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            write(chunk, cursor, cells[i], minY, blockOf(fluids, fluid[cells[i]]));
        }
    }

    private static void write(ChunkAccess chunk, BlockPos.MutableBlockPos cursor, int cell, int minY,
            BlockState state) {
        chunk.setBlockState(cursor.set(chunk.getPos().getMinBlockX() + cell % CHUNK_COLUMNS,
                minY + cell / (CHUNK_COLUMNS * CHUNK_COLUMNS),
                chunk.getPos().getMinBlockZ() + cell / CHUNK_COLUMNS % CHUNK_COLUMNS), state);
    }

    private static boolean windowSolid(TerrainMask[] window, int cell, int lowestY) {
        return window[chunkIndexOf(cell)].solidAt(cell % WINDOW_COLUMNS % CHUNK_COLUMNS,
                lowestY + cell / WINDOW_CELLS,
                cell / WINDOW_COLUMNS % WINDOW_COLUMNS % CHUNK_COLUMNS);
    }

    private static int chunkIndexOf(int cell) {
        return cell / WINDOW_COLUMNS % WINDOW_COLUMNS / CHUNK_COLUMNS * WINDOW_CHUNKS
                + cell % WINDOW_COLUMNS / CHUNK_COLUMNS;
    }

    private static boolean onWindowSide(int cell) {
        int localX = cell % WINDOW_COLUMNS;
        int localZ = cell / WINDOW_COLUMNS % WINDOW_COLUMNS;
        return localX == 0 || localZ == 0 || localX == WINDOW_COLUMNS - 1 || localZ == WINDOW_COLUMNS - 1;
    }

    private static int windowCell(int localX, int localY, int localZ) {
        return localX + localZ * WINDOW_COLUMNS + localY * WINDOW_CELLS;
    }

    private static int windowNeighbour(int cell, int axis, int step, int windowHeight) {
        int localX = cell % WINDOW_COLUMNS;
        int localZ = cell / WINDOW_COLUMNS % WINDOW_COLUMNS;
        int localY = cell / WINDOW_CELLS;
        int nextX = axis == 0 ? localX + step : localX;
        int nextZ = axis == 1 ? localZ + step : localZ;
        int nextY = axis == 2 ? localY + step : localY;
        if (nextX < 0 || nextZ < 0 || nextY < 0
                || nextX >= WINDOW_COLUMNS || nextZ >= WINDOW_COLUMNS || nextY >= windowHeight) {
            return NO_CELL;
        }

        return windowCell(nextX, nextY, nextZ);
    }

    private static boolean get(long[] bits, int index) {
        return (bits[index / Long.SIZE] & 1L << (index % Long.SIZE)) != 0L;
    }

    private static void set(long[] bits, int index) {
        bits[index / Long.SIZE] |= 1L << (index % Long.SIZE);
    }

    static Sweep clearCrumbs(boolean[] solid, byte[] fluid, int height, int solidBlocks, IntConsumer cleared) {
        boolean[] taken = new boolean[solid.length];
        int[] pending = new int[solidBlocks];
        int detached = 0;
        int swept = 0;
        int blocks = 0;

        for (int start = 0; start < solid.length; start++) {
            if (!solid[start] || taken[start]) {
                continue;
            }

            taken[start] = true;
            pending[0] = start;
            int head = 0;
            int tail = 1;
            boolean againstSide = false;

            while (head < tail) {
                int cell = pending[head++];
                int dx = cell % CHUNK_COLUMNS;
                int dz = cell / CHUNK_COLUMNS % CHUNK_COLUMNS;
                if (dx == 0 || dz == 0 || dx == CHUNK_COLUMNS - 1 || dz == CHUNK_COLUMNS - 1) {
                    againstSide = true;
                }

                for (int axis = 0; axis < 3; axis++) {
                    for (int step = -1; step <= 1; step += 2) {
                        int neighbour = neighbourOf(cell, axis, step, height);
                        if (neighbour != NO_CELL && solid[neighbour] && !taken[neighbour]) {
                            taken[neighbour] = true;
                            pending[tail++] = neighbour;
                        }
                    }
                }
            }

            if (againstSide) {
                continue;
            }

            detached++;
            if (tail >= CRUMB_CEILING_BLOCKS) {
                continue;
            }

            swept++;
            blocks += tail;
            for (int i = 0; i < tail; i++) {
                solid[pending[i]] = false;
            }

            Arrays.sort(pending, 0, tail);
            floodCleared(fluid, height, pending, tail);
            for (int i = 0; i < tail; i++) {
                cleared.accept(pending[i]);
            }
        }

        return new Sweep(detached, swept, blocks);
    }

    private static void floodCleared(byte[] fluid, int height, int[] cells, int count) {
        boolean spread = true;
        while (spread) {
            spread = false;
            for (int i = 0; i < count; i++) {
                int cell = cells[i];
                if (fluid[cell] != NO_FLUID) {
                    continue;
                }

                byte around = fluidAround(fluid, height, cell);
                if (around != NO_FLUID) {
                    fluid[cell] = around;
                    spread = true;
                }
            }
        }
    }

    private static byte fluidAround(byte[] fluid, int height, int cell) {
        for (int axis = 0; axis < 3; axis++) {
            for (int step = -1; step <= 1; step += 2) {
                if (axis == 2 && step < 0) {
                    continue;
                }

                int neighbour = neighbourOf(cell, axis, step, height);
                if (neighbour != NO_CELL && fluid[neighbour] != NO_FLUID) {
                    return fluid[neighbour];
                }
            }
        }

        return NO_FLUID;
    }

    private static int neighbourOf(int cell, int axis, int step, int height) {
        int dx = cell % CHUNK_COLUMNS;
        int dz = cell / CHUNK_COLUMNS % CHUNK_COLUMNS;
        int y = cell / (CHUNK_COLUMNS * CHUNK_COLUMNS);
        int nx = axis == 0 ? dx + step : dx;
        int nz = axis == 1 ? dz + step : dz;
        int ny = axis == 2 ? y + step : y;
        if (nx < 0 || nz < 0 || ny < 0 || nx >= CHUNK_COLUMNS || nz >= CHUNK_COLUMNS || ny >= height) {
            return NO_CELL;
        }

        return nx + nz * CHUNK_COLUMNS + ny * CHUNK_COLUMNS * CHUNK_COLUMNS;
    }

    private static int fill(ChunkAccess chunk, boolean[] solid, byte[] fluid, List<BlockState> fluids, int minY) {
        int solidBlocks = 0;

        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            LevelChunkSection section = chunk.getSection(index);
            if (section.hasOnlyAir()) {
                continue;
            }

            int sectionMinY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int y = 0; y < SECTION_BLOCKS; y++) {
                for (int z = 0; z < CHUNK_COLUMNS; z++) {
                    for (int x = 0; x < CHUNK_COLUMNS; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        int cell = x + z * CHUNK_COLUMNS
                                + (sectionMinY + y - minY) * CHUNK_COLUMNS * CHUNK_COLUMNS;
                        FluidState carried = state.getFluidState();
                        if (!carried.isEmpty()) {
                            fluid[cell] = codeOf(fluids, sourceBlockOf(carried));
                        }

                        if (solidCell(state)) {
                            solid[cell] = true;
                            solidBlocks++;
                        }
                    }
                }
            }
        }

        return solidBlocks;
    }

    static boolean solidCell(BlockState state) {
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock);
    }

    static BlockState sourceBlockOf(FluidState fluid) {
        Fluid type = fluid.getType();
        Fluid source = type instanceof FlowingFluid flowing ? flowing.getSource() : type;
        return source.defaultFluidState().createLegacyBlock();
    }

    private static byte codeOf(List<BlockState> fluids, BlockState fluid) {
        int index = fluids.indexOf(fluid);
        if (index < 0) {
            if (fluids.size() >= Byte.MAX_VALUE) {
                return NO_FLUID;
            }

            fluids.add(fluid);
            index = fluids.size() - 1;
        }

        return (byte) (index + 1);
    }

    private static BlockState blockOf(List<BlockState> fluids, byte code) {
        return code == NO_FLUID ? AIR : fluids.get(code - 1);
    }

    private FloatingCrumbs() {
    }
}
