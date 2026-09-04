package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

@Mixin(Biome.class)
public class BiomePrecipitationMixin {
    @WrapMethod(method = "getPrecipitationAt")
    private Biome.Precipitation toroidal$foldForLevellessCaller(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            return original.call(pos);
        }

        if (GenerationTransformerContext.context().wrappedTransformer() != null) {
            return original.call(pos);
        }

        WorldFold bounds = WorldLoopAttachments.wrappedClientBoundsTransformerOf(minecraft.level);
        if (bounds == null) {
            return original.call(pos);
        }

        return GenerationTransformerContext.withTransformer(bounds, () -> original.call(pos));
    }
}
