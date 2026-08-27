package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;

// The client decides for itself whether the weather over a block falls as rain or as snow, and it asks the same
// temperature field the server places ice from. On 1.21.1 the two weather doors — the falling curtain and the rain
// particles — each ask the biome inline, so each method is bound whole. Left unbound they would read the unfolded
// field and draw a straight line of rain against snow along the seam — and disagree with the server about the same
// block. The bounds come from the client bounds store rather than from the level's own transformer, which stays NOOP
// on the client by design; the sampler folds whatever coordinate it is given, however many laps out client space has
// run.
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    private ClientLevel level;

    @WrapMethod(method = "renderSnowAndRain")
    private void toroidal$bindWeatherCurtainTransformer(LightTexture lightTexture, float partialTick,
            double cameraX, double cameraY, double cameraZ, Operation<Void> original) {
        @Nullable WorldFold bounds = toroidal$clientBounds();
        if (bounds == null) {
            original.call(lightTexture, partialTick, cameraX, cameraY, cameraZ);
            return;
        }

        GenerationTransformerContext.runWithTransformer(bounds,
                () -> original.call(lightTexture, partialTick, cameraX, cameraY, cameraZ));
    }

    @WrapMethod(method = "tickRain")
    private void toroidal$bindRainParticleTransformer(Camera camera, Operation<Void> original) {
        @Nullable WorldFold bounds = toroidal$clientBounds();
        if (bounds == null) {
            original.call(camera);
            return;
        }

        GenerationTransformerContext.runWithTransformer(bounds, () -> original.call(camera));
    }

    @Unique
    private @Nullable WorldFold toroidal$clientBounds() {
        return this.level == null ? null : WorldLoopAttachments.wrappedClientBoundsTransformerOf(this.level);
    }
}
