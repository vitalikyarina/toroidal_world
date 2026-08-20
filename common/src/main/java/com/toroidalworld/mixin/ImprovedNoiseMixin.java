package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.PeriodicNoiseSampler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

@Mixin(ImprovedNoise.class)
public class ImprovedNoiseMixin {
    @Shadow
    @Final
    private byte[] p;

    @Shadow
    @Final
    public double xo;

    @Shadow
    @Final
    public double yo;

    @Shadow
    @Final
    public double zo;

    @WrapMethod(method = "noise(DDDDD)D")
    private double toroidal$periodicNoise(double x, double y, double z, double yScale, double yFudge, Operation<Double> original) {
        Context context = GenerationTransformerContext.context();
        WorldLoopTransformer transformer = context.wrappedTransformer();
        if (transformer == null) {
            return original.call(x, y, z, yScale, yFudge);
        }

        return PeriodicNoiseSampler.sample(this.p, this.xo, this.yo, this.zo, transformer,
                context.slotAxes(), context.horizontalScale(), x, y, z, yScale, yFudge);
    }
}
