package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.gen.WorldShapeReport;
import com.toroidalworld.net.PacketTranslator;
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

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void toroidal$publishCurrentServer(CallbackInfo ci) {
        CurrentServer.set((MinecraftServer) (Object) this);
    }

    @Inject(method = "runServer", at = @At("RETURN"))
    private void toroidal$clearCurrentServer(CallbackInfo ci) {
        CurrentServer.clear();
    }

    @Inject(method = "runServer", at = @At("HEAD"))
    private void toroidal$closePacketRewriters(CallbackInfo ci) {
        PacketTranslator.closeRewriters();
    }

    @Inject(method = "createLevels", at = @At("TAIL"))
    private void toroidal$logWorldShape(CallbackInfo ci) {
        for (String line : WorldShapeReport.lines((MinecraftServer) (Object) this)) {
            ToroidalWorld.LOGGER.info(line);
        }
    }

    @WrapOperation(
            method = "setInitialSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Climate$Sampler;findSpawnPosition()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$spawnSearchInBounds(Climate.Sampler sampler, Operation<BlockPos> original,
            @Local(argsOnly = true) ServerLevel level) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(sampler);
        }

        BlockPos found = GenerationTransformerContext.withTransformer(transformer, () -> original.call(sampler));
        return transformer.fold(found);
    }

    @WrapOperation(
            method = "setInitialSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/PlayerRespawnLogic;getSpawnPosInChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/core/BlockPos;"))
    private static @Nullable BlockPos toroidal$searchWrappedChunk(ServerLevel level, ChunkPos chunkPos,
            Operation<@Nullable BlockPos> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(level, chunkPos);
        }

        return original.call(level, transformer.fold(chunkPos));
    }

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
