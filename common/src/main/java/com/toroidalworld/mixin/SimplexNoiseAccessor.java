package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;

@Mixin(SimplexNoise.class)
public interface SimplexNoiseAccessor {
    @Accessor("p")
    int[] toroidal$permutations();
}
