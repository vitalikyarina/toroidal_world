package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.RelocatableBlockEntity;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFold.Folded;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

@Mixin(Level.class)
public class LevelMixin implements TransformerCache {
    @Unique
    private WorldFold toroidal$transformer;

    @WrapOperation(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkSource;getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private @Nullable ChunkAccess toroidal$wrapChunkAccess(ChunkSource chunkSource, int chunkX, int chunkZ,
            ChunkStatus status, boolean loadOrGenerate, Operation<@Nullable ChunkAccess> original) {
        WorldFold transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return original.call(chunkSource, chunkX, chunkZ, status, loadOrGenerate);
        }

        long folded = transformer.foldChunkKey(ChunkPos.pack(chunkX, chunkZ));
        return original.call(chunkSource, ChunkPos.getX(folded), ChunkPos.getZ(folded),
                status, loadOrGenerate);
    }

    @ModifyVariable(method = "getBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapBlockEntityPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @ModifyVariable(method = "removeBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapRemovedBlockEntityPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapPlacedBlockPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void toroidal$wrapBlockEntityIdentity(BlockEntity blockEntity, CallbackInfo ci) {
        WorldFold transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        if (!transformer.isOver(pos)) {
            return;
        }

        ((RelocatableBlockEntity) blockEntity).toroidal$relocate(transformer.fold(pos));
    }

    @WrapOperation(
            method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/LevelEntityGetter;get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V"))
    private void toroidal$entitiesThroughSeam(LevelEntityGetter<Entity> entities, AABB box, Consumer<Entity> output,
            Operation<Void> original) {
        if (!toroidal$crossesSeam(box)) {
            original.call(entities, box, output);
            return;
        }

        List<Folded<AABB>> pieces = toroidal$transformer().split(box);
        if (pieces.size() == 1) {
            original.call(entities, pieces.getFirst().value(), output);
            return;
        }

        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Folded<AABB> piece : pieces) {
            original.call(entities, piece.value(), (Consumer<Entity>) entity -> {
                if (seen.add(entity)) {
                    output.accept(entity);
                }
            });
        }
    }

    @WrapOperation(
            method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/LevelEntityGetter;get(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V"))
    private <U extends Entity> void toroidal$typedEntitiesThroughSeam(LevelEntityGetter<Entity> entities,
            EntityTypeTest<Entity, U> type, AABB box, AbortableIterationConsumer<U> output, Operation<Void> original) {
        if (!toroidal$crossesSeam(box)) {
            original.call(entities, type, box, output);
            return;
        }

        List<Folded<AABB>> pieces = toroidal$transformer().split(box);
        if (pieces.size() == 1) {
            original.call(entities, type, pieces.getFirst().value(), output);
            return;
        }

        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Folded<AABB> piece : pieces) {
            original.call(entities, type, piece.value(), (AbortableIterationConsumer<U>) entity ->
                    seen.add(entity) ? output.accept(entity) : AbortableIterationConsumer.Continuation.CONTINUE);
        }
    }

    @Unique
    private boolean toroidal$crossesSeam(AABB box) {
        WorldFold transformer = toroidal$transformer();
        return transformer.isWrapped() && transformer.crossesBounds(box);
    }

    @Unique
    private BlockPos toroidal$wrap(BlockPos pos) {
        WorldFold transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return pos;
        }

        return transformer.fold(pos);
    }

    @Override
    public WorldFold toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = toroidal$resolveTransformer();
        }

        return this.toroidal$transformer;
    }

    @Unique
    private WorldFold toroidal$resolveTransformer() {
        if (!((Object) this instanceof ServerLevelAccessor accessor)) {
            return WorldFolds.NOOP;
        }

        ServerLevel level = accessor.getLevel();
        return level == (Object) this
                ? toroidal$generatorTransformer(level)
                : WorldLoopAttachments.transformerOf(level);
    }

    @Unique
    private static WorldFold toroidal$generatorTransformer(ServerLevel level) {
        WorldFold transformer =
                ShapedChunkGenerator.wrappedTransformerOf(level.getChunkSource().getGenerator());
        return transformer == null ? WorldFolds.NOOP : transformer;
    }

    @WrapMethod(method = "precipitationAt")
    private Biome.Precipitation toroidal$bindPrecipitationTransformer(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        return GenerationTransformerContext.withTransformer(
                WorldLoopAttachments.noiseTransformerOf((Level) (Object) this), () -> original.call(pos));
    }
}
