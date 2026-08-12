package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.RelocatableBlockEntity;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

// The one gate every read and write of the world passes through: block states, fluids, block entities, setBlock and
// collisions all end up asking the level for a chunk. Wrapping the chunk coordinate here is enough for the block inside
// it to come out right too — a chunk indexes its blocks by the low four bits of the position, which already wrap.
//
// This is the player's own access to the world, not the chunk-loading machinery: tickets and the chunk map talk to the
// chunk source directly and stay untouched.
@Mixin(Level.class)
public class LevelMixin implements TransformerCache {
    @Unique
    private WorldLoopTransformer toroidal$transformer;

    @WrapOperation(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkSource;getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private @Nullable ChunkAccess toroidal$wrapChunkAccess(ChunkSource chunkSource, int chunkX, int chunkZ,
            ChunkStatus status, boolean loadOrGenerate, Operation<@Nullable ChunkAccess> original) {
        WorldLoopTransformer transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return original.call(chunkSource, chunkX, chunkZ, status, loadOrGenerate);
        }

        return original.call(chunkSource, transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ),
                status, loadOrGenerate);
    }

    // Block states survive the wrap for free: a chunk indexes them by the low four bits of the position, which are the
    // same in every copy. Block entities are not so lucky — they live in a map keyed by the whole position, so a chunk
    // reached from beyond the bounds would be searched under a key nothing was ever stored under. The position itself
    // has to be wrapped, not just the chunk it names.
    @ModifyVariable(method = "getBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapBlockEntityPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @ModifyVariable(method = "removeBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapRemovedBlockEntityPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    // A block entity is born inside the chunk's setBlockState, keyed by whatever position it was given.
    @ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapPlacedBlockPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    // The one thing the world files under a position it is handed by the object rather than by its caller: setBlockEntity
    // reads getBlockPos() off the entity itself, so there is no argument to wrap. The chunk it lands in comes out right
    // for free — getChunkAt goes through the gate above — but the key inside that chunk, the key its ticker is registered
    // under, and the coordinate it later writes to NBT all come from the entity's own position. Born a step past the
    // bounds, as a piston pushing across the seam makes it, it is filed under a coordinate no lookup will ever ask for:
    // it never ticks, is never found again, and saves to the region file that way — where vanilla then throws on load.
    //
    // Corrected here because this is the first and only moment the entity meets a level: the constructor that fixes the
    // position has no level to ask whether the world wraps.
    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void toroidal$wrapBlockEntityIdentity(BlockEntity blockEntity, CallbackInfo ci) {
        WorldLoopTransformer transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        if (!transformer.coords.x.isOver(pos.getX()) && !transformer.coords.z.isOver(pos.getZ())) {
            return;
        }

        ((RelocatableBlockEntity) blockEntity).toroidal$relocate(transformer.blocks.wrap(pos));
    }

    // Looking for entities in a box is blind to the seam: the box reaches past the bounds into empty space, while the
    // ground it means to cover is on the other side of the world. Vanilla searches the empty half and finds nobody — a
    // chest does not see the player standing right in front of it, a mob does not see its target. The box is cut into
    // the pieces it really covers and each is searched in turn.
    //
    // The pieces are disjoint, but an entity whose own box straddles the boundary intersects two of them, so a seen-set
    // keeps it from being handed over twice.
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

        List<AABB> pieces = toroidal$transformer().splitAcrossBounds(box);
        if (pieces.size() == 1) {
            original.call(entities, pieces.getFirst(), output);
            return;
        }

        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AABB piece : pieces) {
            original.call(entities, piece, (Consumer<Entity>) entity -> {
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

        List<AABB> pieces = toroidal$transformer().splitAcrossBounds(box);
        if (pieces.size() == 1) {
            original.call(entities, type, pieces.getFirst(), output);
            return;
        }

        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AABB piece : pieces) {
            original.call(entities, type, piece, (AbortableIterationConsumer<U>) entity ->
                    seen.add(entity) ? output.accept(entity) : AbortableIterationConsumer.Continuation.CONTINUE);
        }
    }

    // Asked before the box is cut rather than after, so the overwhelmingly common answer — nothing crosses — costs no
    // list, no spans and no pieces. A level that does not wrap has no seam to cross in the first place.
    @Unique
    private boolean toroidal$crossesSeam(AABB box) {
        WorldLoopTransformer transformer = toroidal$transformer();
        return transformer.isWrapped() && transformer.crossesBounds(box);
    }

    // Every block entity read, every removal and every setBlock in the game reaches this, on the client's own level as
    // much as on the server's. A level that does not wrap has nothing here to correct, and asking the domains says so
    // one virtual call at a time; the level knows the answer outright.
    @Unique
    private BlockPos toroidal$wrap(BlockPos pos) {
        WorldLoopTransformer transformer = toroidal$transformer();
        return transformer.isWrapped() ? transformer.blocks.wrap(pos) : pos;
    }

    // The border is per-level state and never learns which level owns it — it is handed two bare doubles for a centre
    // and measures everything against them. This is the one place the two are in the same room, so the level's shape is
    // stamped on here, and WorldBorderMixin folds its measurements against it.
    //
    // At the return rather than at a creation hook: the transformer is resolved off the level's chunk generator, and a
    // constructor has none to read yet. The identity check makes every call after the first a single reference compare.
    //
    // On Level rather than on ServerLevel, because that is where the game version declares getWorldBorder and an
    // injector reaches only what its target class declares itself. No server check is needed with it: a client level
    // resolves to the very NOOP instance the border's field already holds, so the compare fails and nothing is written
    // — which is the requirement, the client being told the world is infinite.
    @Inject(method = "getWorldBorder", at = @At("RETURN"))
    private void toroidal$bindBorderToLevelShape(CallbackInfoReturnable<WorldBorder> cir) {
        WorldLoopTransformer transformer = toroidal$transformer();
        TransformerHolder border = (TransformerHolder) cir.getReturnValue();
        if (border.toroidal$transformer() != transformer) {
            border.toroidal$setTransformer(transformer);
        }
    }

    // The one place the transformer is actually resolved — transformerOf routes every caller in the mod here, so the
    // resolve runs once per level and everything after it is a field read. Deliberately not volatile — resolution is
    // idempotent, the generator hands back the level's one transformer instance, so a race can only cost a repeated
    // resolve, never a second transformer.
    @Override
    public WorldLoopTransformer toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = toroidal$resolveTransformer();
        }

        return this.toroidal$transformer;
    }

    // The bounds come from the level's own chunk generator. A looped world is created with the Toroidal world shape,
    // which rebuilds the overworld generator as a LoopedChunkGenerator carrying them — and vanilla persists a world's
    // generators. The shape itself is never stored, so the generator is the one thing that can still answer "does this
    // level wrap, and how wide" after a restart. Client levels have no chunk generator and answer NOOP — the client is
    // told the world is infinite; the bounds it may know about live in ClientBoundsHolder, apart from the engine.
    @Unique
    private WorldLoopTransformer toroidal$resolveTransformer() {
        if (!((Object) this instanceof ServerLevel serverLevel)) {
            return WorldLoopTransformer.NOOP;
        }

        if (serverLevel.getChunkSource().getGenerator() instanceof ShapedChunkGenerator shaped) {
            return shaped.transformer();
        }

        return WorldLoopTransformer.NOOP;
    }

    // Whether rain falls on a block is the same temperature field the ice is placed from, asked outside any
    // generation step — so the transformer has to be bound here too, or the sky disagrees with the ground at the seam.
    // On 1.21.1 the level-side primitive is isRainingAt; 26.x later split its body out as precipitationAt.
    // On a client level this binds NOOP, which is correct twice over: the client's engine transformer is NOOP by
    // design, and a bound NOOP shields the call from a leftover binding on the same thread.
    @WrapMethod(method = "isRainingAt")
    private boolean toroidal$bindPrecipitationTransformer(BlockPos pos, Operation<Boolean> original) {
        return GenerationTransformerContext.withTransformer(
                toroidal$precipitationTransformer(), () -> original.call(pos));
    }

    // A client level's own transformer is NOOP by design, so binding it here would leave the client reading the
    // unfolded temperature field — and disagreeing with the server about the same block, worst of all near the seam
    // where client coordinates may sit a whole world width away. What the client does know is the bounds the server
    // told it, which is the same source the weather renderer binds.
    @Unique
    private WorldLoopTransformer toroidal$precipitationTransformer() {
        WorldLoopTransformer clientBounds =
                WorldLoopAttachments.wrappedClientBoundsTransformerOf((Level) (Object) this);
        return clientBounds != null ? clientBounds : toroidal$transformer();
    }
}
