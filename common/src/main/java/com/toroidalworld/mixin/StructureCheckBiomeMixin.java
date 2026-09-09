package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
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
        return GenerationTransformerContext.withTransformer(
                ShapedChunkGenerator.transformerOf(this.chunkGenerator), () -> original.call(pos, structure));
    }
}
