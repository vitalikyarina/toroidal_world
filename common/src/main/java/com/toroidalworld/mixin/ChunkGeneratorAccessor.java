package com.toroidalworld.mixin;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

// The probe of one candidate cell — presence check first, then the start itself — is vanilla's own and stays
// vanilla's; the folded ring scan only needs to call it per cell it names, and vanilla keeps it private.
@Mixin(ChunkGenerator.class)
public interface ChunkGeneratorAccessor {
    @Invoker("getStructureGeneratingAt")
    static @Nullable Pair<BlockPos, Holder<Structure>> toroidal$structureGeneratingAt(
            Set<Holder<Structure>> structures,
            LevelReader level,
            StructureManager structureManager,
            boolean createReference,
            StructurePlacement placement,
            ChunkPos candidate) {
        throw new AssertionError();
    }
}
