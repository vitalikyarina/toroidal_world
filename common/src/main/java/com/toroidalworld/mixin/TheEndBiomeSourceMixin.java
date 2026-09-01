package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(TheEndBiomeSource.class)
public class TheEndBiomeSourceMixin {
    @Shadow
    @Final
    private Holder<Biome> end;

    @Shadow
    @Final
    private Holder<Biome> highlands;

    @Shadow
    @Final
    private Holder<Biome> midlands;

    @Shadow
    @Final
    private Holder<Biome> islands;

    @Shadow
    @Final
    private Holder<Biome> barrens;

    @WrapMethod(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;")
    private Holder<Biome> toroidal$loopedNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
            Operation<Holder<Biome>> original) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(quartX, quartY, quartZ, sampler);
        }

        int blockY = QuartPos.toBlock(quartY);
        long chunk = transformer.foldChunkKey(ChunkPos.pack(
                SectionPos.blockToSectionCoord(QuartPos.toBlock(quartX)),
                SectionPos.blockToSectionCoord(QuartPos.toBlock(quartZ))));
        int chunkX = ChunkPos.getX(chunk);
        int chunkZ = ChunkPos.getZ(chunk);
        if ((long) chunkX * chunkX + (long) chunkZ * chunkZ <= 4096L) {
            return this.end;
        }

        int erosionBlockX = (chunkX * 2 + 1) * 8;
        int erosionBlockZ = (chunkZ * 2 + 1) * 8;
        double heightValue = sampler.erosion().compute(new DensityFunction.SinglePointContext(erosionBlockX, blockY, erosionBlockZ));
        if (heightValue > 0.25) {
            return this.highlands;
        }

        if (heightValue >= -0.0625) {
            return this.midlands;
        }

        return heightValue < -0.21875 ? this.islands : this.barrens;
    }
}
