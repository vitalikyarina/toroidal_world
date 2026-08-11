package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;

// The permutation table the periodic sampler hashes into. Vanilla keeps it private and reads it only through its own
// getValue, which is the method being replaced — so the table has to be reachable from outside it.
@Mixin(SimplexNoise.class)
public interface SimplexNoiseAccessor {
    @Accessor("p")
    int[] toroidal$permutations();
}
