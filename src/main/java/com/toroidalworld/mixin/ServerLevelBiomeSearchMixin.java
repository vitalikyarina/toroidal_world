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

// The one way into the biome search: /locate biome is its only caller, and this level method its only entry — every
// BiomeSource reaches the spiral through here, including the FixedBiomeSource override that answers without touching
// noise at all. Correcting the search where the level is still in hand is what lets one method answer for all of them.
//
// Three things are wrong with it on a looped world, and each is a different kind of wrong.
//
// The search runs straight off the command thread with nothing binding GenerationTransformerContext, so every sample
// computes through the vanilla, non-periodic density functions — the periodic mixins all gate on the bound transformer
// and take the vanilla branch without one. The biomes actually written into chunks come from doCreateBiomes, which is
// bound. The command was answering for a world that was never generated.
//
// The spiral then walks raw coordinates out to 6400 blocks and hands back the raw BlockPos it stopped on. Any world
// narrower than 12800 blocks is left behind on the way, so the reported [x ~ z] — and the /tp @s x ~ z the message
// offers beside it — name ground the world does not have. LocateCommandMixin already folds the distance in that same
// message, which is how this reads in game: a short number next to a coordinate a world away.
//
// And the radius is a search of the whole torus over and over. Rings past half the world can only name places already
// named, at 6400 blocks that is 160801 columns of periodic noise on the server thread, and a biome the world does not
// hold has no early exit to save it.
//
// The ranking needs nothing of its own. The wrapped field is periodic with the width of the world — the invariant
// LoopedChunkGenerator's base-height cache already stands on — so a column and its wrapped twin answer alike, and the
// spiral's rings are therefore rings of folded distance: ring r names exactly the places within 32r of the origin the
// short way round. The first hit is already the nearest copy through the seam; it only has to be folded before it is
// spoken.
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

    // How far the spiral has to reach to have seen the whole world: half of the widest axis, counted in the steps the
    // search actually takes. Each axis answers for itself and the larger wins, because covering that one covers the
    // other; an axis that does not close answers with a count no radius reaches, so a world looped in one axis only
    // keeps vanilla's own radius. In longs, because that answer is Integer.MAX_VALUE and the step multiplies it.
    @Unique
    private static int toroidal$radiusToCoverTheWorld(WorldLoopTransformer transformer, int maxSearchRadius,
            int sampleResolutionHorizontal) {
        int steps = Math.max(
                transformer.coords.x.stepsToCoverTheWorld(sampleResolutionHorizontal),
                transformer.coords.z.stepsToCoverTheWorld(sampleResolutionHorizontal));
        return (int) Math.min(maxSearchRadius, (long) steps * sampleResolutionHorizontal);
    }
}
