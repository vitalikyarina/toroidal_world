package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.net.PacketReach;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.player.SeamSnap;
import com.toroidalworld.storage.SeamRespawnData;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Unique
    private static final double PARTICLE_RANGE = PacketReach.PARTICLE.blocks();

    @Unique
    private static final double OVERRIDDEN_PARTICLE_RANGE = PacketReach.FORCED_PARTICLE.blocks();

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

        return ChunkPos.asLong(transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
    }

    @ModifyVariable(method = "setDefaultSpawnPos(Lnet/minecraft/core/BlockPos;F)V", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$storeWorldSpawnInsideBounds(BlockPos spawnPos) {
        return SeamRespawnData.insideBounds((ServerLevel) (Object) this, spawnPos);
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
