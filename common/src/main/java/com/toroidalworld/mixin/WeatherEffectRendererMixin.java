package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
    @WrapMethod(method = "getPrecipitationAt")
    private Biome.Precipitation toroidal$bindPrecipitationTransformer(
            Level level, BlockPos pos, Operation<Biome.Precipitation> original) {
        @Nullable WorldFold bounds = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        if (bounds == null) {
            return original.call(level, pos);
        }

        return GenerationTransformerContext.withTransformer(bounds, () -> original.call(level, pos));
    }
}
