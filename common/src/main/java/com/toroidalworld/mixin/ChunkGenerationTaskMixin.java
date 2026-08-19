package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import org.slf4j.Logger;

import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ChunkGenerationTask;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GeneratingChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;

@Mixin(ChunkGenerationTask.class)
public class ChunkGenerationTaskMixin {
    @WrapOperation(
            method = "create",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/StaticCache2D;create(IIILnet/minecraft/util/StaticCache2D$Initializer;)Lnet/minecraft/util/StaticCache2D;"))
    private static StaticCache2D<GenerationChunkHolder> toroidal$foldSeamSlots(
            int centerX,
            int centerZ,
            int range,
            StaticCache2D.Initializer<GenerationChunkHolder> initializer,
            Operation<StaticCache2D<GenerationChunkHolder>> original,
            @Local(argsOnly = true) GeneratingChunkMap chunkMap) {
        if (!(chunkMap instanceof ChunkMap map)) {
            return original.call(centerX, centerZ, range, initializer);
        }

        ServerLevel level = ((LevelHolder) (Object) map).toroidal$level();
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(centerX, centerZ, range, initializer);
        }

        String levelName = level.dimension().location().toString();
        StaticCache2D.Initializer<GenerationChunkHolder> folding = (slotX, slotZ) -> toroidal$slotFor(
                map, transformer, levelName, initializer, centerX, centerZ, slotX, slotZ);
        return original.call(centerX, centerZ, range, folding);
    }

    @Unique
    private static final Logger toroidal$LOGGER = LogUtils.getLogger();

    @Unique
    private static GenerationChunkHolder toroidal$slotFor(
            ChunkMap map,
            WorldLoopTransformer transformer,
            String levelName,
            StaticCache2D.Initializer<GenerationChunkHolder> initializer,
            int centerX,
            int centerZ,
            int slotX,
            int slotZ) {
        if (slotX == centerX && slotZ == centerZ) {
            return initializer.get(slotX, slotZ);
        }

        if (!transformer.chunks.x.isOver(slotX) && !transformer.chunks.z.isOver(slotZ)) {
            return initializer.get(slotX, slotZ);
        }

        int wrappedX = transformer.chunks.x.wrap(slotX);
        int wrappedZ = transformer.chunks.z.wrap(slotZ);

        if (map.getUpdatingChunkIfPresent(ChunkPos.asLong(wrappedX, wrappedZ)) == null) {
            toroidal$LOGGER.warn(
                    "missing_folded_holder level={} slot_x={} slot_z={} wrapped_x={} wrapped_z={} center_x={} center_z={}",
                    levelName, slotX, slotZ, wrappedX, wrappedZ, centerX, centerZ);
            return initializer.get(slotX, slotZ);
        }

        return initializer.get(wrappedX, wrappedZ);
    }
}
