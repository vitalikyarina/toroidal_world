package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;

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

    @Unique
    private WorldFold toroidal$transformer() {
        return this.chunkGenerator instanceof ShapedChunkGenerator shaped
                ? shaped.transformer()
                : WorldFolds.NOOP;
    }
}
