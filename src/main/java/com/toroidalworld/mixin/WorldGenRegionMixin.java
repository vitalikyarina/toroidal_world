package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.core.WorldLoopTransformer;
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

// Once ChunkGenerationTaskMixin has folded a slot, a write addressed one chunk past the bounds lands in the real chunk
// on the other side of the world — but it still carries the position it was addressed with. Whatever indexes by the low
// bits survives that untouched (block states, heightmaps, the post-processing shorts), so those are deliberately left
// alone. What does NOT survive is every use of the position as a KEY: a block entity is filed under its whole BlockPos,
// and a chest keyed a world away from the chunk holding it is the corruption this card exists to avoid.
//
// The rule is not "wrap out-of-bounds positions" but "state the key in the frame of the chunk that received the write".
// Those coincide when a slot was folded and are identical when it was not, so a fallback slot stays exactly vanilla
// without this mixin having to know why the fold declined.
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements LevelHolder {
    @Shadow
    @Final
    private ServerLevel level;

    @Override
    public ServerLevel toroidal$level() {
        return this.level;
    }

    // The key as the receiving chunk would file it. Equal to the argument whenever the write went where its coordinates
    // said, so the ordinary path allocates nothing.
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

    // A block entity is created at the position it was asked for and then filed under its own getBlockPos.
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

    // The placeholder a proto-chunk stores instead of a live block entity carries the coordinates in its own tag, and
    // getBlockEntity later rebuilds the block entity from them.
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

    // POI records are filed by section. PoiManagerMixin folds the searches, not the writes, so a record stored at a raw
    // out-of-bounds position sits in a section nothing will ever look at.
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

    // Reading a block entity is also where one gets BUILT: the placeholder a proto-chunk stored is materialised here and
    // filed straight back into the chunk under the position it was built with. Rebasing only the lookup left the build
    // itself on the raw position, so the block entity landed in the right chunk under a key a world away from it — two
    // per generated world, every time, always on the seam. Both constructors have to be rebased, not just the read.
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

    // An entity carries world coordinates of its own; the chunk it is filed in does not correct them.
    @WrapOperation(
            method = "addFreshEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;addEntity(Lnet/minecraft/world/entity/Entity;)V"))
    private void toroidal$addEntityInChunkFrame(ChunkAccess chunk, Entity entity, Operation<Void> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer != null && transformer.vectors.isOver(entity.position())) {
            entity.setPos(transformer.vectors.wrap(entity.position()));
        }

        original.call(chunk, entity);
    }
}
