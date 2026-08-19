package com.toroidalworld.mixin;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

@Mixin(ServerLevel.class)
public class ServerLevelBiomeSearchMixin {
    @WrapMethod(method = "findClosestBiome3d")
    private @Nullable Pair<BlockPos, Holder<Biome>> toroidal$biomeSearchThroughSeam(
            Predicate<Holder<Biome>> biomeTest,
            BlockPos origin,
            int maxSearchRadius,
            int sampleResolutionHorizontal,
            int sampleResolutionVertical,
            Operation<@Nullable Pair<BlockPos, Holder<Biome>>> original) {
        WorldLoopTransformer transformer =
                WorldLoopAttachments.wrappedTransformerOf((ServerLevel) (Object) this);
        if (transformer == null) {
            return original.call(biomeTest, origin, maxSearchRadius, sampleResolutionHorizontal,
                    sampleResolutionVertical);
        }

        int searchRadius = toroidal$radiusToCoverTheWorld(transformer, maxSearchRadius, sampleResolutionHorizontal);
        Pair<BlockPos, Holder<Biome>> found = GenerationTransformerContext.withTransformer(transformer,
                () -> original.call(biomeTest, origin, searchRadius, sampleResolutionHorizontal,
                        sampleResolutionVertical));

        return found == null ? null : Pair.of(transformer.blocks.wrap(found.getFirst()), found.getSecond());
    }

    @Unique
    private static int toroidal$radiusToCoverTheWorld(WorldLoopTransformer transformer, int maxSearchRadius,
            int sampleResolutionHorizontal) {
        int steps = Math.max(
                transformer.coords.x.stepsToCoverTheWorld(sampleResolutionHorizontal),
                transformer.coords.z.stepsToCoverTheWorld(sampleResolutionHorizontal));
        return (int) Math.min(maxSearchRadius, (long) steps * sampleResolutionHorizontal);
    }
}
