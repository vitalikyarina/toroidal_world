package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(SurfaceSystem.class)
public class SurfaceSystemMixin {
    @Shadow
    @Final
    private NormalNoise badlandsPillarNoise;

    @Shadow
    @Final
    private NormalNoise badlandsPillarRoofNoise;

    @Shadow
    @Final
    private NormalNoise icebergPillarNoise;

    @Shadow
    @Final
    private NormalNoise icebergPillarRoofNoise;

    @WrapOperation(
            method = {"erodedBadlandsExtension", "frozenOceanExtension"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/synth/NormalNoise;getValue(DDD)D"))
    private double toroidal$rawCoordinateNoise(NormalNoise noise, double x, double y, double z, Operation<Double> original) {
        double scale = toroidal$scaleOf(noise);
        Context generation = GenerationTransformerContext.context();
        if (scale == NoiseConstants.UNSCALED || !generation.transformer().isWrapped()) {
            return original.call(noise, x, y, z);
        }

        try (Context.ScaleScope _ = generation.withScale(scale)) {
            return original.call(noise, x / scale, y, z / scale);
        }
    }

    @Unique
    private double toroidal$scaleOf(NormalNoise noise) {
        if (noise == this.badlandsPillarNoise) {
            return NoiseConstants.BADLANDS_PILLAR_SCALE;
        }
        if (noise == this.badlandsPillarRoofNoise) {
            return NoiseConstants.BADLANDS_PILLAR_ROOF_SCALE;
        }
        if (noise == this.icebergPillarNoise) {
            return NoiseConstants.ICEBERG_PILLAR_SCALE;
        }
        if (noise == this.icebergPillarRoofNoise) {
            return NoiseConstants.ICEBERG_PILLAR_ROOF_SCALE;
        }
        return NoiseConstants.UNSCALED;
    }
}
