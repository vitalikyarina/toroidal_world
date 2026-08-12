package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

// The client decides for itself whether the weather over a block falls as rain or as snow, and it asks the same
// temperature field the server places ice from — both the weather curtain and the rain particles come in through this
// helper. Left unbound it would read the unfolded field and draw a straight line of rain against snow along the seam —
// and disagree with the server about the same block. The bounds come from the level the caller hands over rather than
// from that level's own transformer, which stays NOOP on the client by design; the sampler folds whatever coordinate
// it is given, however many laps out client space has run.
@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
    @WrapMethod(method = "getPrecipitationAt")
    private Biome.Precipitation toroidal$bindPrecipitationTransformer(
            Level level, BlockPos pos, Operation<Biome.Precipitation> original) {
        @Nullable WorldLoopTransformer bounds = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        if (bounds == null) {
            return original.call(level, pos);
        }

        return GenerationTransformerContext.withTransformer(bounds, () -> original.call(level, pos));
    }
}
