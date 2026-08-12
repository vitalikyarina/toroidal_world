package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

// Until the server's bounds payload arrives the holder answers NOOP, so a bounds reader honestly sees an unwrapped
// world — which is also the truth on a server that never sends the payload. A fresh level (a dimension change) starts
// back at NOOP the same way.
@Mixin(ClientLevel.class)
public class ClientLevelMixin implements ClientBoundsHolder {
    @Unique
    private WorldLoopTransformer toroidal$clientBounds = WorldLoopTransformer.NOOP;

    @Override
    public WorldLoopTransformer toroidal$clientBounds() {
        return this.toroidal$clientBounds;
    }

    @Override
    public void toroidal$setClientBounds(WorldLoopTransformer transformer) {
        this.toroidal$clientBounds = transformer;
    }

    // The client decides for itself whether the weather over a block falls as rain or as snow, and it asks the same
    // temperature field the server places ice from. Left unbound it would read the unfolded field and draw a straight
    // line of rain against snow along the seam — and disagree with the server about the same block. The bounds come
    // from the holder above rather than from the level's own transformer, which stays NOOP on the client by design;
    // the sampler folds whatever coordinate it is given, however many laps out client space has run.
    @WrapMethod(method = "getPrecipitationAt")
    private Biome.Precipitation toroidal$bindPrecipitationTransformer(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        return GenerationTransformerContext.withTransformer(this.toroidal$clientBounds, () -> original.call(pos));
    }
}
