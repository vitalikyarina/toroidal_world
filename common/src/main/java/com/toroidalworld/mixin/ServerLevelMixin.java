package com.toroidalworld.mixin;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.net.PacketReach;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.player.SeamSnap;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Unique
    private static final double PARTICLE_RANGE = PacketReach.PARTICLE.blocks();

    @Unique
    private static final double OVERRIDDEN_PARTICLE_RANGE = PacketReach.FORCED_PARTICLE.blocks();

    @Unique
    private static final double GLOBAL_EVENT_RANGE = 32.0;

    @Unique
    private static final double BLOCK_DESTRUCTION_RANGE = 32.0;

    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindLevelToTickContainers(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        ((LevelBindable) level.getBlockTicks()).toroidal$bindLevel(level);
        ((LevelBindable) level.getFluidTicks()).toroidal$bindLevel(level);
        ((LevelBindable) level.getRaids()).toroidal$bindLevel(level);
        ((LevelBindable) this.entityManager).toroidal$bindLevel(level);
    }

    @Inject(method = "getWorldBorder", at = @At("RETURN"))
    private void toroidal$bindBorderToLevelShape(CallbackInfoReturnable<WorldBorder> cir) {
        WorldLoopTransformer transformer = WorldLoopAttachments.transformerOf((ServerLevel) (Object) this);
        TransformerHolder border = (TransformerHolder) cir.getReturnValue();
        if (border.toroidal$transformer() != transformer) {
            border.toroidal$setTransformer(transformer);
        }
    }

    @Inject(method = "tickNonPassenger", at = @At("TAIL"))
    private void toroidal$wrapEntityIntoBounds(Entity entity, CallbackInfo ci) {
        if (entity instanceof Player) {
            return;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf((ServerLevel) (Object) this);
        if (transformer == null) {
            return;
        }

        if (transformer.vectors.isOver(entity.position())) {
            Vec3 wrapped = transformer.vectors.wrap(entity.position());
            SeamSnap.withPassengers(entity, wrapped.subtract(entity.position()));
        }
    }

    @WrapOperation(
            method = "waitForEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ChunkPos;rangeClosed(Lnet/minecraft/world/level/ChunkPos;I)Ljava/util/stream/Stream;"))
    private Stream<ChunkPos> toroidal$waitOnPhysicalChunks(
            ChunkPos center, int radius, Operation<Stream<ChunkPos>> original) {
        Stream<ChunkPos> square = original.call(center, radius);
        ServerLevel level = (ServerLevel) (Object) this;
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return square;
        }

        return square.map(transformer.chunks::wrap).distinct();
    }

    @ModifyVariable(method = "shouldTickBlocksAt(J)Z", at = @At("HEAD"), argsOnly = true)
    private long toroidal$tickingChunkThroughSeam(long chunkPos) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf((ServerLevel) (Object) this);
        if (transformer == null) {
            return chunkPos;
        }

        int chunkX = ChunkPos.getX(chunkPos);
        int chunkZ = ChunkPos.getZ(chunkPos);
        if (!transformer.chunks.x.isOver(chunkX) && !transformer.chunks.z.isOver(chunkZ)) {
            return chunkPos;
        }

        return ChunkPos.pack(transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
    }

    @WrapMethod(method = "sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z")
    private boolean toroidal$particlesThroughSeam(ServerPlayer player, boolean overrideLimiter, double x, double y, double z,
            Packet<?> packet, Operation<Boolean> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(player, overrideLimiter, x, y, z, packet);
        }

        if (player.level() != level) {
            return false;
        }

        double range = overrideLimiter ? OVERRIDDEN_PARTICLE_RANGE : PARTICLE_RANGE;
        Vec3 center = Vec3.atCenterOf(player.blockPosition());
        if (transformer.coords.sqrDistToBounds(center.x, center.y, center.z, x, y, z) >= range * range) {
            return false;
        }

        player.connection.send(packet);
        return true;
    }

    @WrapMethod(method = "globalLevelEvent")
    private void toroidal$globalEventThroughSeam(int type, BlockPos pos, int data, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null || !level.getGameRules().get(GameRules.GLOBAL_SOUND_EVENTS)) {
            original.call(type, pos, data);
            return;
        }

        Vec3 rawEventPos = Vec3.atCenterOf(pos);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            Vec3 listenerPos = player.position();
            Vec3 soundPos;
            if (player.level() == level) {
                Vec3 eventPos = transformer.vectors.nearestCopy(listenerPos, rawEventPos);
                if (player.distanceToSqr(eventPos) < GLOBAL_EVENT_RANGE * GLOBAL_EVENT_RANGE) {
                    soundPos = eventPos;
                } else {
                    Vec3 directionToEvent = eventPos.subtract(listenerPos).normalize();
                    soundPos = listenerPos.add(directionToEvent.scale(GLOBAL_EVENT_RANGE));
                }
            } else {
                soundPos = listenerPos;
            }

            player.connection.send(new ClientboundLevelEventPacket(type, BlockPos.containing(soundPos), data, true));
        }
    }

    @WrapMethod(method = "destroyBlockProgress")
    private void toroidal$blockCracksThroughSeam(int id, BlockPos blockPos, int progress, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            original.call(id, blockPos, progress);
            return;
        }

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || player.getId() == id) {
                continue;
            }

            double distanceSqr = transformer.coords.sqrDistToBounds(player.getX(), player.getY(), player.getZ(),
                    blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (distanceSqr >= BLOCK_DESTRUCTION_RANGE * BLOCK_DESTRUCTION_RANGE) {
                continue;
            }

            player.connection.send(new ClientboundBlockDestructionPacket(id, blockPos, progress));
        }
    }

    @WrapMethod(method = "tickPrecipitation")
    private void toroidal$bindPrecipitationTransformer(BlockPos pos, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        GenerationTransformerContext.runWithTransformer(
                WorldLoopAttachments.transformerOf(level), () -> original.call(pos));
    }
}
