package com.toroidalworld.engine.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.CrumbSweepCache;
import com.toroidalworld.accessors.TerrainMaskCache;
import com.toroidalworld.accessors.TerrainMaskHolder;
import com.toroidalworld.api.v1.gen.TerrainSnapshot;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.ShapedChunkGenerator;
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

    private static final int CHUNK_COLUMNS = CoordinateConstants.CHUNK_WIDTH;

    private static final int SECTION_BLOCKS = 16;

    private static final int WINDOW_CHUNKS = 3;

    private static final int WINDOW_COLUMNS = WINDOW_CHUNKS * CHUNK_COLUMNS;

    private static final int CENTRE_INDEX = WINDOW_CHUNKS + 1;

    private static final int CLEARED_SEED = 64;

    static final byte NO_FLUID = 0;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static final long[] NO_POSITIONS = new long[0];

    record Sweep(int detached, int swept, int blocks) {
    }

    private static final class Cleared {
        private final CellGrid grid;
        private final int lowestY;
        private int[] cells = new int[CLEARED_SEED];
        private int count;

        private Cleared(CellGrid grid, int lowestY) {
            this.grid = grid;
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

    private static TerrainMasks terrainMasksOf(ServerLevel level) {
        return ((TerrainMaskCache) level).toroidal$terrainMasks();
    }

    private static @Nullable TerrainMask terrainMaskOf(ChunkAccess chunk) {
        return chunk instanceof TerrainMaskHolder holder ? holder.toroidal$terrainMask() : null;
    }

    private static void attachTerrainMask(ChunkAccess chunk, TerrainMask mask) {
        if (chunk instanceof TerrainMaskHolder holder) {
            holder.toroidal$terrainMask(mask);
        }
    }

    public static void sweep(ServerLevel level, ChunkAccess chunk) {
        if (!sweepsCrumbs(level)) {
            return;
        }

        int minY = chunk.getMinY();
        CellGrid grid = new CellGrid(CHUNK_COLUMNS, chunk.getHeight());
        boolean[] solid = new boolean[grid.cells()];
        byte[] fluid = new byte[solid.length];
        List<BlockState> fluids = new ArrayList<>();
        int solidBlocks = fill(chunk, grid, solid, fluid, fluids, minY);
        if (solidBlocks > 0) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            clearCrumbs(grid, solid, fluid, solidBlocks,
                    cell -> write(chunk, grid, cursor, cell, minY, blockOf(fluids, fluid[cell])));
        }

        attachTerrainMask(chunk, TerrainMask.of(solid, chunk.getPos(), minY, grid));
    }

    public static void registerMask(ServerLevel level, ChunkAccess chunk) {
        TerrainMask mask = terrainMaskOf(chunk);
        if (mask != null) {
            terrainMasksOf(level).put(chunk.getPos(), mask);
        }
    }

    public static void sweepAcross(ServerLevel level, ChunkAccess chunk,
            StaticCache2D<GenerationChunkHolder> chunks) {
        if (!sweepsCrumbs(level)) {
            return;
        }

        TerrainMasks masks = terrainMasksOf(level);
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

    static void sweepWindow(ChunkAccess chunk, TerrainMask[] window) {
        Cleared cleared = crumbCells(window);
        if (cleared == null || cleared.count == 0) {
            return;
        }

        TerrainMask centre = window[CENTRE_INDEX];
        CellGrid grid = cleared.grid;
        int[] cells = new int[cleared.count];
        for (int i = 0; i < cleared.count; i++) {
            int cell = cleared.cells[i];
            cells[i] = centre.cellOf(centreLocalX(grid, cell), cleared.lowestY + grid.layer(cell),
                    centreLocalZ(grid, cell));
        }

        clearInChunk(chunk, cells, cleared.count);
    }

    public static @Nullable TerrainSnapshot snapshotOf(ChunkAccess chunk) {
        return terrainMaskOf(chunk);
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
        CellGrid grid = cleared.grid;
        long[] positions = new long[cleared.count];
        for (int i = 0; i < cleared.count; i++) {
            int cell = cleared.cells[i];
            positions[i] = BlockPos.asLong(pos.getMinBlockX() + centreLocalX(grid, cell),
                    cleared.lowestY + grid.layer(cell),
                    pos.getMinBlockZ() + centreLocalZ(grid, cell));
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

        CellGrid grid = new CellGrid(WINDOW_COLUMNS, highestY - lowestY + 1);
        BitSet taken = new BitSet(grid.cells());
        BitSet rejected = new BitSet(grid.cells());
        int[] pending = new int[CRUMB_CEILING_BLOCKS];
        Cleared cleared = new Cleared(grid, lowestY);

        for (int y = centre.lowestY(); y <= centre.highestY(); y++) {
            for (int localZ = 0; localZ < CHUNK_COLUMNS; localZ++) {
                for (int localX = 0; localX < CHUNK_COLUMNS; localX++) {
                    if (!centre.solidAt(localX, y, localZ)) {
                        continue;
                    }

                    int start = grid.cell(CHUNK_COLUMNS + localX, y - lowestY, CHUNK_COLUMNS + localZ);
                    if (!taken.get(start)) {
                        takeCrumb(window, taken, rejected, pending, cleared, start);
                    }
                }
            }
        }

        return cleared;
    }

    private static int centreLocalX(CellGrid window, int cell) {
        return window.localX(cell) - CHUNK_COLUMNS;
    }

    private static int centreLocalZ(CellGrid window, int cell) {
        return window.localZ(cell) - CHUNK_COLUMNS;
    }

    private static void takeCrumb(TerrainMask[] window, BitSet taken, BitSet rejected, int[] pending,
            Cleared cleared, int start) {
        CellGrid grid = cleared.grid;
        taken.set(start);
        pending[0] = start;
        int head = 0;
        int tail = 1;
        boolean overCeiling = false;

        while (head < tail && !overCeiling) {
            int cell = pending[head++];
            if (grid.onSide(cell)) {
                overCeiling = true;
                break;
            }

            for (int axis = 0; axis < CellGrid.AXES && !overCeiling; axis++) {
                for (int step = -1; step <= 1; step += 2) {
                    int neighbour = grid.neighbour(cell, axis, step);
                    if (neighbour == CellGrid.NO_CELL
                            || !windowSolid(window, grid, neighbour, cleared.lowestY)) {
                        continue;
                    }

                    if (rejected.get(neighbour) || tail == pending.length) {
                        overCeiling = true;
                        break;
                    }

                    if (!taken.get(neighbour)) {
                        taken.set(neighbour);
                        pending[tail++] = neighbour;
                    }
                }
            }
        }

        if (overCeiling || tail >= CRUMB_CEILING_BLOCKS) {
            for (int i = 0; i < tail; i++) {
                rejected.set(pending[i]);
            }

            return;
        }

        TerrainMask centre = window[CENTRE_INDEX];
        for (int i = 0; i < tail; i++) {
            int cell = pending[i];
            if (chunkIndexOf(grid, cell) == CENTRE_INDEX
                    && centre.untouchedAt(centreLocalX(grid, cell), cleared.lowestY + grid.layer(cell),
                            centreLocalZ(grid, cell))) {
                cleared.add(cell);
            }
        }
    }

    private static void clearInChunk(ChunkAccess chunk, int[] cells, int count) {
        int minY = chunk.getMinY();
        CellGrid grid = new CellGrid(CHUNK_COLUMNS, chunk.getHeight());
        boolean[] solid = new boolean[grid.cells()];
        byte[] fluid = new byte[solid.length];
        List<BlockState> fluids = new ArrayList<>();
        fill(chunk, grid, solid, fluid, fluids, minY);

        Arrays.sort(cells, 0, count);
        floodCleared(grid, fluid, cells, count);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            write(chunk, grid, cursor, cells[i], minY, blockOf(fluids, fluid[cells[i]]));
        }
    }

    private static void write(ChunkAccess chunk, CellGrid grid, BlockPos.MutableBlockPos cursor, int cell,
            int minY, BlockState state) {
        chunk.setBlockState(cursor.set(chunk.getPos().getMinBlockX() + grid.localX(cell),
                minY + grid.layer(cell),
                chunk.getPos().getMinBlockZ() + grid.localZ(cell)), state);
    }

    private static boolean windowSolid(TerrainMask[] window, CellGrid grid, int cell, int lowestY) {
        return window[chunkIndexOf(grid, cell)].solidAt(grid.localX(cell) % CHUNK_COLUMNS,
                lowestY + grid.layer(cell), grid.localZ(cell) % CHUNK_COLUMNS);
    }

    private static int chunkIndexOf(CellGrid grid, int cell) {
        return grid.localZ(cell) / CHUNK_COLUMNS * WINDOW_CHUNKS + grid.localX(cell) / CHUNK_COLUMNS;
    }

    static Sweep clearCrumbs(CellGrid grid, boolean[] solid, byte[] fluid, int solidBlocks, IntConsumer cleared) {
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
                if (grid.onSide(cell)) {
                    againstSide = true;
                }

                for (int axis = 0; axis < CellGrid.AXES; axis++) {
                    for (int step = -1; step <= 1; step += 2) {
                        int neighbour = grid.neighbour(cell, axis, step);
                        if (neighbour != CellGrid.NO_CELL && solid[neighbour] && !taken[neighbour]) {
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
            floodCleared(grid, fluid, pending, tail);
            for (int i = 0; i < tail; i++) {
                cleared.accept(pending[i]);
            }
        }

        return new Sweep(detached, swept, blocks);
    }

    private static void floodCleared(CellGrid grid, byte[] fluid, int[] cells, int count) {
        boolean spread = true;
        while (spread) {
            spread = false;
            for (int i = 0; i < count; i++) {
                int cell = cells[i];
                if (fluid[cell] != NO_FLUID) {
                    continue;
                }

                byte around = fluidAround(grid, fluid, cell);
                if (around != NO_FLUID) {
                    fluid[cell] = around;
                    spread = true;
                }
            }
        }
    }

    private static byte fluidAround(CellGrid grid, byte[] fluid, int cell) {
        for (int axis = 0; axis < CellGrid.AXES; axis++) {
            for (int step = -1; step <= 1; step += 2) {
                if (axis == CellGrid.AXIS_Y && step < 0) {
                    continue;
                }

                int neighbour = grid.neighbour(cell, axis, step);
                if (neighbour != CellGrid.NO_CELL && fluid[neighbour] != NO_FLUID) {
                    return fluid[neighbour];
                }
            }
        }

        return NO_FLUID;
    }

    private static int fill(ChunkAccess chunk, CellGrid grid, boolean[] solid, byte[] fluid,
            List<BlockState> fluids, int minY) {
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
                        int cell = grid.cell(x, sectionMinY + y - minY, z);
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
