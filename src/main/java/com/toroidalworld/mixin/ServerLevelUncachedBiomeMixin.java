package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

// What the server answers when asked for a biome it has no chunk for. LevelReader.getNoiseBiome reads the loaded chunk
// when there is one and falls through to here when there is not, so every level.getBiome() over ungenerated ground
// arrives at this method — and it samples the biome source straight off the calling thread, with nothing binding
// GenerationTransformerContext. The periodic mixins gate on that binding, so the answer came from the vanilla,
// non-periodic field: the biome named for a place is not the biome that will be there once the chunk generates.
//
// The single server-side sink for it. WorldGenRegion.getUncachedNoiseBiome delegates here, so a query made during
// generation lands on the same method (already bound by its step, and a second scoped bind of the same transformer
// changes nothing), and the loaded-chunk path never reaches it at all.
//
// The coordinates need no folding. The field is periodic with the width of the world, so a quart column past the bounds
// answers exactly as its wrapped twin does — the sample is already the right one, it only had to be taken from the
// right field.
@Mixin(ServerLevel.class)
public class ServerLevelUncachedBiomeMixin {
    @WrapMethod(method = "getUncachedNoiseBiome")
    private Holder<Biome> toroidal$sampleThisWorldsBiomeField(int quartX, int quartY, int quartZ,
            Operation<Holder<Biome>> original) {
        return GenerationTransformerContext.withTransformer(
                WorldLoopAttachments.transformerOf((ServerLevel) (Object) this),
                () -> original.call(quartX, quartY, quartZ));
    }
}
