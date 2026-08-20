package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

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
