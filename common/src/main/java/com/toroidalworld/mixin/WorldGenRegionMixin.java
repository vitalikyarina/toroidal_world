package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements LevelHolder {
    @Shadow
    @Final
    private ServerLevel level;

    @Override
    public ServerLevel toroidal$level() {
        return this.level;
    }

    @Unique
    private BlockPos toroidal$keyIn(ChunkAccess chunk, BlockPos pos) {
        ChunkPos chunkPos = chunk.getPos();
        if (chunkPos.x() == SectionPos.blockToSectionCoord(pos.getX())
                && chunkPos.z() == SectionPos.blockToSectionCoord(pos.getZ())) {
            return pos;
        }

        BlockPos key = new BlockPos(
                SectionPos.sectionToBlockCoord(chunkPos.x(), SectionPos.sectionRelative(pos.getX())),
                pos.getY(),
                SectionPos.sectionToBlockCoord(chunkPos.z(), SectionPos.sectionRelative(pos.getZ())));

        return key;
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/EntityBlock;newBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity toroidal$createBlockEntityInChunkFrame(
            EntityBlock block, BlockPos pos, BlockState state, Operation<BlockEntity> original, @Local ChunkAccess chunk) {
        return original.call(block, this.toroidal$keyIn(chunk, pos), state);
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V"))
    private void toroidal$removeBlockEntityInChunkFrame(ChunkAccess chunk, BlockPos pos, Operation<Void> original) {
        original.call(chunk, this.toroidal$keyIn(chunk, pos));
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockEntityNbt(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void toroidal$storeNbtInChunkFrame(ChunkAccess chunk, CompoundTag tag, Operation<Void> original, @Local BlockPos pos) {
        BlockPos key = this.toroidal$keyIn(chunk, pos);
        tag.putInt("x", key.getX());
        tag.putInt("z", key.getZ());
        original.call(chunk, tag);
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;updatePOIOnBlockStateChange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void toroidal$updatePoiInChunkFrame(
            ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState, Operation<Void> original, @Local ChunkAccess chunk) {
        original.call(level, this.toroidal$keyIn(chunk, pos), oldState, newState);
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity toroidal$readBlockEntityInChunkFrame(
            ChunkAccess chunk, BlockPos pos, Operation<BlockEntity> original) {
        return original.call(chunk, this.toroidal$keyIn(chunk, pos));
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntityNbt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/nbt/CompoundTag;"))
    private @Nullable CompoundTag toroidal$readBlockEntityNbtInChunkFrame(
            ChunkAccess chunk, BlockPos pos, Operation<CompoundTag> original) {
        return original.call(chunk, this.toroidal$keyIn(chunk, pos));
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/EntityBlock;newBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity toroidal$rebuildBlockEntityInChunkFrame(
            EntityBlock block, BlockPos pos, BlockState state, Operation<BlockEntity> original, @Local ChunkAccess chunk) {
        return original.call(block, this.toroidal$keyIn(chunk, pos), state);
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadStatic(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity toroidal$loadBlockEntityInChunkFrame(
            BlockPos pos,
            BlockState state,
            CompoundTag tag,
            HolderLookup.Provider registries,
            Operation<BlockEntity> original,
            @Local ChunkAccess chunk) {
        return original.call(this.toroidal$keyIn(chunk, pos), state, tag, registries);
    }

    @WrapOperation(
            method = "addFreshEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;addEntity(Lnet/minecraft/world/entity/Entity;)V"))
    private void toroidal$addEntityInChunkFrame(ChunkAccess chunk, Entity entity, Operation<Void> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer != null && transformer.isOver(entity.position())) {
            entity.setPos(transformer.fold(entity.position()));
        }

        original.call(chunk, entity);
    }
}
