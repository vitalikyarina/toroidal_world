package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;

// Every structure search — /locate structure, an explorer map, an eye of ender, a dolphin led to treasure — funnels into
// checkStart, and for a chunk that has never generated it falls through to this one method. Everything above it is
// already covered: the candidate rings are folded and bounded (ChunkGeneratorRandomSpreadSearchMixin), the stronghold ring list
// is folded into the world (ChunkGeneratorStructureStateMixin). This is the last place on that chain still sampling
// noise for itself.
//
// findValidGenerationPoint asks two questions, and only one of them was honest. The height comes through
// getFirstOccupiedHeight, which reaches the noise via LoopedChunkGenerator.iterateNoiseColumn — bound there, and
// restored on the way out, which is exactly what leaves the biome sample that follows it unbound. That sample runs
// straight off the searching thread, so every periodic mixin sees no transformer and takes the vanilla, non-periodic
// branch. The very same check during real generation is bound (ChunkStatusTasksMixin around generateStructureStarts),
// so the search and the world were answering from two different biome maps.
//
// A wrong "no" is the damaging one: the structure becomes invisible to every search, and StructureCheck.featureChecks
// remembers that verdict per chunk key for the rest of the session, so asking again cannot change it. A wrong "yes"
// self-corrects — the chunk is generated at STRUCTURE_STARTS, no start is found and the search moves on — at the price
// of generating a chunk for nothing.
//
// Bound around canCreateStructure rather than checkStart because the three answers above it — the loaded-chunk cache,
// the storage scan and the placement's own chunk restrictions — touch no noise at all, and a chunk that already exists
// never reaches here.
@Mixin(StructureCheck.class)
public class StructureCheckBiomeMixin {
    @Shadow
    @Final
    private ChunkGenerator chunkGenerator;

    @WrapMethod(method = "canCreateStructure")
    private boolean toroidal$validateAgainstThisWorldsBiomes(ChunkPos pos, Structure structure,
            Operation<Boolean> original) {
        return GenerationTransformerContext.withTransformer(toroidal$transformer(),
                () -> original.call(pos, structure));
    }

    // One StructureCheck per level, holding that level's own generator, so the bounds are read from the generator the
    // check was built with rather than looked up per call.
    //
    // Bound unconditionally, NOOP included, for the reason ChunkStatusTasksMixin gives: this runs on the server thread
    // for a command and on the shared worldgen pool for a loot table, and a scoped bind that overwrites and restores
    // leaves the thread as clean as it found it. Every periodic mixin gates on isWrapped(), so a NOOP binding is the
    // vanilla path byte-for-byte.
    @Unique
    private WorldLoopTransformer toroidal$transformer() {
        return this.chunkGenerator instanceof ShapedChunkGenerator shaped
                ? shaped.transformer()
                : WorldLoopTransformer.NOOP;
    }
}
