package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.command.SeamCommandErrors;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;

import net.minecraft.core.QuartPos;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

// Same ambiguity as /fill, answered the same way. This one hands its failures back as Either.right rather than
// throwing, so the refusal is returned in that shape too — the command prints it exactly as it prints "volume too
// large". The narrower overload is the one that does the work; the short one delegates to it.
//
// The corners this command is given are not the region it fills: biomes are stored per four blocks, so each corner is
// first pulled down to the quart grid it belongs to. Judging the region the command built rather than the corners it
// was handed keeps one answer to what is being filled instead of two that differ by up to three blocks — and costs no
// bounding box of our own. The injection sits at the first use of that region, which is also before the volume check,
// so a region both ambiguous and oversized is answered with the reason that actually stops it.
//
// This one could declare its arguments ahead of the callback — every type in its signature is public — but reads them
// through @Local like /fill and /clone next door, which cannot. Three neighbours written the same way are worth more
// than one of them matching the rest of the mod.
@Mixin(FillBiomeCommand.class)
public class FillBiomeCommandMixin {
    @Inject(
            method = "fill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;Ljava/util/function/Predicate;Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;getXSpan()I",
                    ordinal = 0),
            cancellable = true)
    private static void toroidal$refuseRegionAcrossSeam(
            CallbackInfoReturnable<Either<Integer, CommandSyntaxException>> cir,
            @Local(argsOnly = true) ServerLevel level,
            @Local BoundingBox region) {
        CommandSyntaxException refusal = SeamCommandErrors.refusalForAmbiguousRegion(
                WorldLoopAttachments.wrappedTransformerOf(level), region);
        if (refusal == null) {
            return;
        }

        cir.setReturnValue(Either.right(refusal));
    }

    // Alone among the region commands, this one does not walk the region it was given. It collects the chunks the region
    // covers — through the level, so a chunk named past the bounds comes back as the real one on the other side — and
    // then hands each of them a resolver that vanilla builds around `chunk.getPos()`. That position is the physical
    // chunk's, and the resolver tests it against the region: a region reaching past the bounds is compared with
    // coordinates from the far side of the world, matches nothing, and the command reports filling zero blocks without
    // an error. /fill and /clone are untouched by this — they iterate the region's own coordinates and let the level
    // resolve each one.
    //
    // So the coordinate is moved into the region's frame rather than the region into the world's: whole laps of the
    // world are added to it until it lands at or after the region's own low edge, which is the only copy that can be
    // inside a region no wider than the world — and for a wider one every copy is, because such a region covers the
    // whole axis anyway. The chunk still reads and writes the biome the coordinate names, because a lap is a whole
    // number of chunks and the section indexes its biomes by the low bits of the quart — the same reason a block
    // written past the bounds lands correctly in the wrapped chunk.
    //
    // Only an axis the region actually leaves is moved, so the ordinary in-bounds fill hands vanilla its own resolver
    // untouched, and an axis that does not wrap is never a candidate.
    @WrapOperation(
            method = "fill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;Ljava/util/function/Predicate;Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;fillBiomesFromNoise(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/world/level/biome/Climate$Sampler;)V"))
    private static void toroidal$fillInTheRegionsFrame(ChunkAccess chunk, BiomeResolver resolver,
            Climate.Sampler sampler, Operation<Void> original,
            @Local(argsOnly = true) ServerLevel level, @Local BoundingBox region) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            original.call(chunk, resolver, sampler);
            return;
        }

        boolean foldX = toroidal$leavesTheWorld(transformer.coords.x, region.minX(), region.maxX());
        boolean foldZ = toroidal$leavesTheWorld(transformer.coords.z, region.minZ(), region.maxZ());
        if (!foldX && !foldZ) {
            original.call(chunk, resolver, sampler);
            return;
        }

        int regionMinX = region.minX();
        int regionMinZ = region.minZ();
        BiomeResolver inFrame = (quartX, quartY, quartZ, resolverSampler) -> resolver.getNoiseBiome(
                foldX ? toroidal$quartInRegionsFrame(transformer.coords.x, regionMinX, quartX) : quartX,
                quartY,
                foldZ ? toroidal$quartInRegionsFrame(transformer.coords.z, regionMinZ, quartZ) : quartZ,
                resolverSampler);

        original.call(chunk, inFrame, sampler);
    }

    private static boolean toroidal$leavesTheWorld(WrapDomain domain, int minCoord, int maxCoord) {
        return domain.isOver(minCoord) || domain.isOver(maxCoord);
    }

    // The lap arithmetic is done in blocks because that is the unit the bounds are kept in, and converts back exactly:
    // a world is a whole number of chunks wide, so a lap is a whole number of quarts.
    private static int toroidal$quartInRegionsFrame(WrapDomain domain, int regionMinCoord, int quart) {
        int block = QuartPos.toBlock(quart);
        int inFrame = domain.wrapFrom(regionMinCoord, block);
        return quart + QuartPos.fromBlock(inFrame - block);
    }
}
