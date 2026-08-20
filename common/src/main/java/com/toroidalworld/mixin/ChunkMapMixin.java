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
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.core.HolderGetter;
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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
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

    @Override
    public void toroidal$dropTrackedChunks(ServerPlayer player) {
        this.applyChunkTrackingView(player, ChunkTrackingView.EMPTY);
    }

    @Override
    public void toroidal$resendTrackedChunks(ServerPlayer player) {
        this.updateChunkTracking(player);
    }

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

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/RandomState;create("
                            + "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;"
                            + "Lnet/minecraft/core/HolderGetter;J)"
                            + "Lnet/minecraft/world/level/levelgen/RandomState;"))
    private RandomState toroidal$bindRouterBuild(
            NoiseGeneratorSettings settings,
            HolderGetter<NormalNoise.NoiseParameters> noiseParameters,
            long seed,
            Operation<RandomState> original,
            @Local(argsOnly = true) ChunkGenerator generator) {
        return GenerationTransformerContext.withRouterBuild(ShapedChunkGenerator.wrappedTransformerOf(generator),
                () -> original.call(settings, noiseParameters, seed));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindLevelToTickets(CallbackInfo ci) {
        ((LevelBindable) this.getDistanceManager()).toroidal$bindLevel(this.level);
    }

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

    @WrapOperation(
            method = "updateChunkTracking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkTrackingView;of(Lnet/minecraft/world/level/ChunkPos;I)Lnet/minecraft/server/level/ChunkTrackingView;"))
    private ChunkTrackingView toroidal$trackWrapped(ChunkPos center, int viewDistance, Operation<ChunkTrackingView> original) {
        ChunkTrackingView view = original.call(center, viewDistance);
        ((TransformerHolder) (Object) view).toroidal$setTransformer(WorldLoopAttachments.transformerOf(this.level));
        return view;
    }

    @WrapMethod(method = "getPlayerViewDistance")
    private int toroidal$clampLoopedViewDistance(ServerPlayer player, Operation<Integer> original) {
        int vanilla = original.call(player);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        return transformer == null ? vanilla : transformer.limitViewDistance(vanilla);
    }

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
