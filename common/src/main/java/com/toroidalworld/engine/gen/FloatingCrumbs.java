package com.toroidalworld.engine.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.noise.TerrainCeiling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
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

    static final byte NO_FLUID = 0;

    private static final int NO_CELL = -1;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    record Sweep(int detached, int swept, int blocks) {
    }

    public static boolean installedOn(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return ShapedChunkGenerator.wrappedTransformerOf(generator) != null
                && generator instanceof NoiseBasedChunkGenerator noise
                && TerrainCeiling.ceiling(noise.generatorSettings().value()) != null;
    }

    public static void sweep(ServerLevel level, ChunkAccess chunk) {
        if (!WorldLoopAttachments.sweepsCrumbs(level)) {
            return;
        }

        int minY = chunk.getMinBuildHeight();
        int height = chunk.getHeight();
        boolean[] solid = new boolean[CHUNK_COLUMNS * CHUNK_COLUMNS * height];
        byte[] fluid = new byte[solid.length];
        List<BlockState> fluids = new ArrayList<>();
        int solidBlocks = fill(chunk, solid, fluid, fluids, minY);
        if (solidBlocks == 0) {
            return;
        }

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        clearCrumbs(solid, fluid, height, solidBlocks, cell -> chunk.setBlockState(
                cursor.set(minX + cell % CHUNK_COLUMNS,
                        minY + cell / (CHUNK_COLUMNS * CHUNK_COLUMNS),
                        minZ + cell / CHUNK_COLUMNS % CHUNK_COLUMNS),
                blockOf(fluids, fluid[cell]), false));
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
