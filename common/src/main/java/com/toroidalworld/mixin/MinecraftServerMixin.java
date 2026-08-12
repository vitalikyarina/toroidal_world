package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.WorldShapeReport;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.CurrentServer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.toroidalworld.storage.SeamRespawnData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.storage.ServerLevelData;

// The world spawn at both ends: where vanilla chooses it, and where it is written down.
//
// Vanilla picks the world spawn by asking the climate sampler for the best-fitting spot, then walking a square spiral of
// chunks around it looking for somewhere to stand. Two things in that break in a looped world, and both are single
// calls rather than the shape of the method: the sampler runs before any chunk step has bound the transformer, so it
// reads non-periodic noise and answers for terrain that will never exist; and neither it nor the spiral knows about the
// bounds, so in a 512-wide world the search happily wanders out to X=400.
//
// Wrapping those two calls is what makes the search honest, and the write below is what makes its answer keepable.
// Replacing the whole method — which is what cancelling LevelEvent.CreateSpawnPosition amounted to — also threw away
// everything vanilla does *after* the spawn is chosen: the bonus chest, and the load-listener stages for the
// spawn-preparation screen. That is how a world created with Bonus Chest ticked quietly produced no chest. Leave
// vanilla's method running and only correct what it asks the world.
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    // runServer is the server thread's whole life: published before initServer loads the first level — so a chunk read
    // during world load already finds it — and cleared only when the thread unwinds past its own finally.
    @Inject(method = "runServer", at = @At("HEAD"))
    private void toroidal$publishCurrentServer(CallbackInfo ci) {
        CurrentServer.set((MinecraftServer) (Object) this);
    }

    @Inject(method = "runServer", at = @At("RETURN"))
    private void toroidal$clearCurrentServer(CallbackInfo ci) {
        CurrentServer.clear();
    }

    // The world-shape lines a bug report needs, at the TAIL of createLevels: every level and its generator exist by
    // then, and the world-load section of the log has only just begun — so the lines sit where a report's excerpt
    // starts, on the dedicated server as much as in singleplayer. An unwrapped world contributes no lines at all.
    @Inject(method = "createLevels", at = @At("TAIL"))
    private void toroidal$logWorldShape(CallbackInfo ci) {
        for (String line : WorldShapeReport.lines((MinecraftServer) (Object) this)) {
            ToroidalWorld.LOGGER.info(line);
        }
    }

    // The sampler has no idea which dimension it serves and no argument to tell it, so the transformer is bound around
    // the call the way every other out-of-step worldgen query binds it. The answer is then folded into the bounds: with
    // periodic noise X=400 in a 512-wide world *is* X=-112, so folding is the correct reading of the result rather than
    // a clamp over a wrong one.
    @WrapOperation(
            method = "setInitialSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Climate$Sampler;findSpawnPosition()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$spawnSearchInBounds(Climate.Sampler sampler, Operation<BlockPos> original,
            @Local(argsOnly = true) ServerLevel level) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(sampler);
        }

        BlockPos found = GenerationTransformerContext.withTransformer(transformer, () -> original.call(sampler));
        return transformer.blocks.wrap(found);
    }

    // The spiral steps one chunk at a time from the chosen chunk, and near the seam its ring runs straight off the edge;
    // those chunks are really on the other side of the world.
    @WrapOperation(
            method = "setInitialSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/PlayerRespawnLogic;getSpawnPosInChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/core/BlockPos;"))
    private static @Nullable BlockPos toroidal$searchWrappedChunk(ServerLevel level, ChunkPos chunkPos,
            Operation<@Nullable BlockPos> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(level, chunkPos);
        }

        return original.call(level, transformer.chunks.wrap(chunkPos));
    }

    // The world spawn a new world starts with never passes ServerLevel.setDefaultSpawnPos: setInitialSpawn writes it
    // into the level data directly, three times over as the search narrows, and the last write is the one that
    // survives. So the bounds are settled here, at the write itself — not on the two searches above, which are wrapped
    // so that the search reads real ground rather than to keep a coordinate in the world, and which are not the only
    // way one can arrive. ServerLevelData.setSpawn is the true sink underneath all three, but it is handed neither a
    // server nor a level and so cannot tell which dimension's bounds it is holding; this is the innermost point that
    // still knows.
    @WrapOperation(
            method = "setInitialSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/ServerLevelData;setSpawn(Lnet/minecraft/core/BlockPos;F)V"))
    private static void toroidal$storeInitialSpawnInsideBounds(ServerLevelData levelData, BlockPos spawnPos,
            float spawnAngle, Operation<Void> original, @Local(argsOnly = true) ServerLevel level) {
        original.call(levelData, SeamRespawnData.insideBounds(level, spawnPos), spawnAngle);
    }
}
