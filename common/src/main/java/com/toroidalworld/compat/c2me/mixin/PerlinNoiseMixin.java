package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.PeriodicOctaveSampler;
import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

// The C2ME-shaped twin of com.toroidalworld.mixin.PerlinNoiseMixin, and the reason that one goes quiet: C2ME's
// opts/math overwrites the three-argument getValue with its own octave loop, which no longer delegates to the
// five-argument overload the mod wraps. The wrapper stays applied and stops being reached — no error, no warning, and
// the octave factor never reaches the context.
//
// What the seam showed for it: ImprovedNoise still got the folded lattice, but with coordinates already multiplied by
// the octave factor and folded through PerlinNoise.wrap, so every octave's phase slipped. The fine detail still read
// as terrain while continentalness and erosion did not tile at all — deep ocean on one side of the seam and land on
// the other.
//
// The same statement, then, made where C2ME moved the loop. An unwrapped level falls through to C2ME's own optimised
// walk, which is left exactly as it is.
@Mixin(PerlinNoise.class)
public class PerlinNoiseMixin {
    @Shadow
    @Final
    private ImprovedNoise[] noiseLevels;

    @Shadow
    @Final
    private DoubleList amplitudes;

    @Shadow
    @Final
    private double lowestFreqValueFactor;

    @Shadow
    @Final
    private double lowestFreqInputFactor;

    @WrapMethod(method = "@MixinSquared:Handler")
    @TargetHandler(
            mixin = "com.ishland.c2me.opts.math.mixin.MixinOctavePerlinNoiseSampler",
            name = "getValue")
    private double toroidal$periodicValue(double x, double y, double z, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(x, y, z);
        }

        // The zeros are what C2ME's loop passes to every octave, kept so the two walks differ in nothing but the fold.
        return PeriodicOctaveSampler.sample(generation, this.noiseLevels, this.amplitudes,
                this.lowestFreqInputFactor, this.lowestFreqValueFactor, x, y, z, 0.0, 0.0);
    }
}
