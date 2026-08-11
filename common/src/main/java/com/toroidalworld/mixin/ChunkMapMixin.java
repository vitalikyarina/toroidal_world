package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.ChunkResender;
import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.accessors.SeamDriveScheduler;
import com.toroidalworld.accessors.TrackedEntityRefresher;
import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.SeamDriveRequest;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityAccess;

@Mixin(ChunkMap.class)
public class ChunkMapMixin implements LevelHolder, ChunkResender, SeamDriveScheduler, TrackedEntityRefresher {
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    private void updateChunkTracking(ServerPlayer player) {
        throw new AssertionError();
    }

    @Shadow
    private void applyChunkTrackingView(ServerPlayer player, ChunkTrackingView next) {
        throw new AssertionError();
    }

    @Shadow
    private void markChunkPendingToSend(ServerPlayer player, ChunkPos pos) {
        throw new AssertionError();
    }

    @Shadow
    private static void dropChunk(ServerPlayer player, ChunkPos pos) {
        throw new AssertionError();
    }

    // Forget everything the player currently holds: difference(current, EMPTY) is all-removals, so every tracked chunk
    // gets a ClientboundForgetLevelChunkPacket — translated around the mirror as it stands now, which is why the teleport
    // hook calls this before the move, while that mirror still names the copies the client is actually holding.
    @Override
    public void toroidal$dropTrackedChunks(ServerPlayer player) {
        this.applyChunkTrackingView(player, ChunkTrackingView.EMPTY);
    }

    // Re-queue every chunk in the player's view: with the view left EMPTY by the drop above, difference(EMPTY, next) is
    // all-additions, so all are sent again — now around the new mirror, once the teleport has moved it.
    @Override
    public void toroidal$resendTrackedChunks(ServerPlayer player) {
        this.updateChunkTracking(player);
    }

    // The named chunks only, through the same per-chunk forget and send the view difference uses — the client keeps
    // everything else it holds.
    @Override
    public void toroidal$dropChunks(ServerPlayer player, List<ChunkPos> chunks) {
        for (ChunkPos chunkPos : chunks) {
            dropChunk(player, chunkPos);
        }
    }

    @Override
    public void toroidal$resendChunks(ServerPlayer player, List<ChunkPos> chunks) {
        for (ChunkPos chunkPos : chunks) {
            this.markChunkPendingToSend(player, chunkPos);
        }
    }

    // Vanilla's own decision, re-asked for one player — the first half of ChunkMap.move, which the teleport path never
    // reaches. The self case needs no branch: updatePlayer returns immediately for the player's own tracked entity, and
    // who is shown THAT entity is already refreshed before it broadcasts, in ChunkMap.tick's section-changed arm.
    //
    // Unwrapped dimensions keep vanilla's timing byte-for-byte: there the client and the server share one space, so a
    // tick of stale tracking costs nothing but an entity update drawn where it belongs.
    @Override
    public void toroidal$refreshTrackedEntities(ServerPlayer player) {
        if (WorldLoopAttachments.wrappedTransformerOf(this.level) == null) {
            return;
        }

        ChunkMap chunkMap = (ChunkMap) (Object) this;
        for (ChunkMap.TrackedEntity tracked : chunkMap.entityMap.values()) {
            tracked.updatePlayer(player);
        }
    }

    // The other writer of the radius the tracker gates entity visibility on. Vanilla re-applies the chunk view for
    // every player here and leaves the entity decision standing on the distance that has just been replaced, so the
    // traffic those pairs keep producing is measured against a bound they were never gated on. Taking the decision
    // again in the same loop iteration is what keeps the two from ever naming different radii; the loop itself sits
    // inside vanilla's own "the distance really moved" branch, so an unchanged setting costs nothing.
    @Inject(
            method = "setServerViewDistance",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap;updateChunkTracking(Lnet/minecraft/server/level/ServerPlayer;)V",
                    shift = At.Shift.AFTER))
    private void toroidal$refreshTrackingOnServerViewChange(int newViewDistance, CallbackInfo ci,
            @Local ServerPlayer player) {
        this.toroidal$refreshTrackedEntities(player);
    }

    @Shadow
    public DistanceManager getDistanceManager() {
        throw new AssertionError();
    }

    // The cache a generation task carries is, in vanilla, a plain map from a holder's own position to that holder — and
    // applyStep leans on it, re-reading the holder it was just handed by that holder's own position. A folded slot files
    // the wrapped neighbour under its RAW key instead, so the identity no longer holds and the lookup walks off the
    // square: "Requested out of range value (-8,1) from StaticCache2D[-3, -8, 18, 13]".
    //
    // The step runs on the holder it was given. Saying so restores exactly what vanilla means by this line and owes
    // nothing to the wrap math — which is why it is stated here rather than repaired with coordinates.
    @WrapOperation(
            method = "applyStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StaticCache2D;get(II)Ljava/lang/Object;"))
    private Object toroidal$stepRunsOnItsOwnHolder(
            StaticCache2D<GenerationChunkHolder> cache,
            int chunkX,
            int chunkZ,
            Operation<Object> original,
            @Local(argsOnly = true) GenerationChunkHolder chunkHolder) {
        if (WorldLoopAttachments.wrappedTransformerOf(this.level) == null) {
            return original.call(cache, chunkX, chunkZ);
        }

        return chunkHolder;
    }

    // Every promotion a chunk can receive is decided here: prepareAccessibleChunk (range 1) turns it into a FULL chunk
    // and is what loads its entities, prepareTickingChunk (range 1) is what makes getChunkToSend hand it to a player,
    // and prepareEntityTickingChunk (range 2) starts its entities ticking. All three ask that a square around the chunk
    // reach a status, and all three walk that square in raw coordinates.
    //
    // For a chunk on the bounds the raw square names keys no holder answers for — the future settles on a failed
    // result, scheduleFullChunkPromotion's ifSuccess never runs, and the promotion simply never arrives. Nothing throws
    // and nothing is logged: the chunk generates, becomes a LevelChunk, and then sits there un-promoted. That is one
    // bug wearing two faces — the world hangs on spawn waiting for entities that will never load, and the band along
    // the seam is never sent to the client at all.
    //
    // The neighbours of a chunk at the bounds ARE the chunks across the seam, so naming them physically is what the
    // range meant all along — the same statement already made for the ticket graphs and the generation cache.
    @WrapOperation(
            method = "getChunkRangeFuture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;asLong(II)J"))
    private long toroidal$rangeOverPhysicalChunks(int chunkX, int chunkZ, Operation<Long> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original.call(chunkX, chunkZ);
        }

        return original.call(transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
    }

    @Unique
    private final ConcurrentLinkedQueue<SeamDriveRequest> toroidal$driveRequests = new ConcurrentLinkedQueue<>();

    // A foreign task may wait on a cross-seam holder but never generate it (GenerationChunkHolderMixin), so somebody
    // has to: vanilla gives a task only to a chunk somebody asks the scheduler for, and the foreign task's ask went to
    // the holder instead. The declined CAS files the request from a worker thread; it is drained here because
    // runGenerationTasks runs after runAllUpdates has refreshed every highestAllowedStatus — the request is not refused
    // for a status the holder is about to be allowed — and on the server thread, the only one permitted to touch the
    // pending-task queue. scheduleChunkGenerationTask dedupes on its own: an existing task at the same or a later
    // target is left alone, so repeated requests are cheap no-ops.
    @Override
    public void toroidal$requestDrive(GenerationChunkHolder holder, ChunkStatus status) {
        if (WorldLoopAttachments.wrappedTransformerOf(this.level) == null) {
            return;
        }

        this.toroidal$driveRequests.add(new SeamDriveRequest(holder, status));
    }

    @Inject(method = "runGenerationTasks", at = @At("HEAD"))
    private void toroidal$driveWaitedOnHolders(CallbackInfo ci) {
        ChunkMap scheduler = (ChunkMap) (Object) this;
        SeamDriveRequest request;
        while ((request = this.toroidal$driveRequests.poll()) != null) {
            request.holder().scheduleChunkGenerationTask(request.status(), scheduler);
        }
    }

    @Override
    public ServerLevel toroidal$level() {
        return this.level;
    }

    // The ticket graphs are built without any reference to the level they serve; this is the first moment both exist.
    // The distance manager passes the level on to every graph it owns.
    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindLevelToTickets(CallbackInfo ci) {
        ((LevelBindable) this.getDistanceManager()).toroidal$bindLevel(this.level);
    }

    // The tracking layer must never be stamped with a raw out-of-bounds position. Mid-tick, an entity that has just
    // crossed the seam still sits at its pre-wrap coordinate (the wrap runs at tick end), and a section or view centre
    // taken from it seeds tickets, the section memory and the view in the phantom frame — on the next move the whole
    // tracking state then relocates a world over, and every crossing churns. Projecting the reads onto the wrapped
    // position keeps every tracking key canonical no matter when the tracker happens to run.
    @WrapOperation(
            method = "updatePlayerStatus",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/entity/EntityAccess;)Lnet/minecraft/core/SectionPos;"))
    private SectionPos toroidal$canonicalStatusSection(EntityAccess entity, Operation<SectionPos> original) {
        return toroidal$canonical(original.call(entity));
    }

    @WrapOperation(
            method = "updatePlayerPos",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/entity/EntityAccess;)Lnet/minecraft/core/SectionPos;"))
    private SectionPos toroidal$canonicalPosSection(EntityAccess entity, Operation<SectionPos> original) {
        return toroidal$canonical(original.call(entity));
    }

    @WrapOperation(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/entity/EntityAccess;)Lnet/minecraft/core/SectionPos;"))
    private SectionPos toroidal$canonicalMoveSection(EntityAccess entity, Operation<SectionPos> original) {
        return toroidal$canonical(original.call(entity));
    }

    @Unique
    private SectionPos toroidal$canonical(SectionPos section) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return section;
        }

        return SectionPos.of(
                transformer.chunks.x.wrap(section.x()), section.y(), transformer.chunks.z.wrap(section.z()));
    }

    @WrapOperation(
            method = "updateChunkTracking",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos toroidal$canonicalViewCenter(ServerPlayer player, Operation<ChunkPos> original) {
        ChunkPos pos = original.call(player);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        return transformer == null ? pos : transformer.chunks.wrap(pos);
    }

    // The view is built by a static factory that never sees the level, so the transformer is handed to it here — the
    // one place views are created.
    @WrapOperation(
            method = "updateChunkTracking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkTrackingView;of(Lnet/minecraft/world/level/ChunkPos;I)Lnet/minecraft/server/level/ChunkTrackingView;"))
    private ChunkTrackingView toroidal$trackWrapped(ChunkPos center, int viewDistance, Operation<ChunkTrackingView> original) {
        ChunkTrackingView view = original.call(center, viewDistance);
        ((TransformerHolder) (Object) view).toroidal$setTransformer(WorldLoopAttachments.transformerOf(this.level));
        return view;
    }

    // The client mapping shows every chunk at its representation nearest the player, which is unambiguous only while
    // everything a player holds is closer than half the world around. The setting is already clamped where it is
    // written (PlayerListMixin) — but the level is CONSTRUCTED with an initial distance that bypasses that setter, so
    // the invariant is enforced once more at the one place every view decision is read from. Same formula, owned by
    // the transformer; dimensions that do not loop are untouched.
    @WrapMethod(method = "getPlayerViewDistance")
    private int toroidal$clampLoopedViewDistance(ServerPlayer player, Operation<Integer> original) {
        int vanilla = original.call(player);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        return transformer == null ? vanilla : transformer.limitViewDistance(vanilla);
    }

    // Mob spawning asks how far a chunk is from a player; across the seam the plain distance is a whole world wide, so
    // a chunk right behind the player would never spawn anything. The player arrives as the entity rather than as a
    // position on this game version, which changes only where the two coordinates are read from.
    @WrapOperation(
            method = "playerIsCloseEnoughForSpawning",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;euclideanDistanceSquared(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/entity/Entity;)D"))
    private double toroidal$wrappedSpawnDistance(ChunkPos chunkPos, Entity player, Operation<Double> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original.call(chunkPos, player);
        }

        double chunkCenterX = SectionPos.sectionToBlockCoord(chunkPos.x, 8);
        double chunkCenterZ = SectionPos.sectionToBlockCoord(chunkPos.z, 8);
        double vanilla = original.call(chunkPos, player);
        double folded = transformer.coords.sqrDistToBounds(
                player.getX(), 0.0, player.getZ(), chunkCenterX, 0.0, chunkCenterZ);
        return folded;
    }
}
