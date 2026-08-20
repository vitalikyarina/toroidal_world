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

    @WrapMethod(method = "getPrecipitationAt")
    private Biome.Precipitation toroidal$bindPrecipitationTransformer(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        return GenerationTransformerContext.withTransformer(this.toroidal$clientBounds, () -> original.call(pos));
    }
}
