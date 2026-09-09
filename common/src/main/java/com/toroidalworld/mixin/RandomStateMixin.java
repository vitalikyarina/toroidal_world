package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.noise.GenerationHooks;
import com.toroidalworld.engine.noise.GenerationTransformerContext;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(RandomState.class)
public class RandomStateMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;"
            + "Lnet/minecraft/core/HolderGetter;J)V", at = @At("RETURN"))
    private void toroidal$runGenerationHooks(NoiseGeneratorSettings settings,
            HolderGetter<NormalNoise.NoiseParameters> noises, long seed, CallbackInfo callback) {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        if (fold != null) {
            GenerationHooks.runAtRandomState((RandomState) (Object) this, fold, settings.seaLevel());
        }
    }
}
