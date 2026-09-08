package com.toroidalworld.engine.gen;

import java.util.function.IntConsumer;

import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.noise.TerrainCeiling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

public final class FloatingCrumbs {
    public static final int CRUMB_CEILING_BLOCKS = 32;

    private static final int CHUNK_COLUMNS = 16;

    private static final int SECTION_BLOCKS = 16;

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

        int minY = chunk.getMinY();
        int height = chunk.getHeight();
        boolean[] solid = new boolean[CHUNK_COLUMNS * CHUNK_COLUMNS * height];
        int solidBlocks = fill(chunk, solid, minY);
        if (solidBlocks == 0) {
            return;
        }

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        clearCrumbs(solid, height, solidBlocks, cell -> chunk.setBlockState(
                cursor.set(minX + cell % CHUNK_COLUMNS,
                        minY + cell / (CHUNK_COLUMNS * CHUNK_COLUMNS),
                        minZ + cell / CHUNK_COLUMNS % CHUNK_COLUMNS),
                AIR));
    }

    static Sweep clearCrumbs(boolean[] solid, int height, int solidBlocks, IntConsumer cleared) {
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
                int y = cell / (CHUNK_COLUMNS * CHUNK_COLUMNS);
                if (dx == 0 || dz == 0 || dx == CHUNK_COLUMNS - 1 || dz == CHUNK_COLUMNS - 1) {
                    againstSide = true;
                }

                for (int axis = 0; axis < 3; axis++) {
                    for (int step = -1; step <= 1; step += 2) {
                        int nx = axis == 0 ? dx + step : dx;
                        int nz = axis == 1 ? dz + step : dz;
                        int ny = axis == 2 ? y + step : y;
                        if (nx < 0 || nz < 0 || ny < 0 || nx >= CHUNK_COLUMNS || nz >= CHUNK_COLUMNS
                                || ny >= height) {
                            continue;
                        }

                        int neighbour = nx + nz * CHUNK_COLUMNS + ny * CHUNK_COLUMNS * CHUNK_COLUMNS;
                        if (solid[neighbour] && !taken[neighbour]) {
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
                cleared.accept(pending[i]);
            }
        }

        return new Sweep(detached, swept, blocks);
    }

    private static int fill(ChunkAccess chunk, boolean[] solid, int minY) {
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
                        if (!state.isAir() && state.getFluidState().isEmpty()) {
                            solid[x + z * CHUNK_COLUMNS
                                    + (sectionMinY + y - minY) * CHUNK_COLUMNS * CHUNK_COLUMNS] = true;
                            solidBlocks++;
                        }
                    }
                }
            }
        }

        return solidBlocks;
    }

    private FloatingCrumbs() {
    }
}
