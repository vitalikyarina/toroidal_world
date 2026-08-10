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
import com.toroidalworld.player.SeamSnap;
import com.toroidalworld.probe.ReshapeProbe;
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

// Deciding who is close enough to see or hear something is a distance test, and in a looped world the plain distance
// lies: a block a step away across the seam sits a whole world apart. The packet would never even be sent.
@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    // Shared with the packet translation, which holds a particle's translated position to the very radius this gate
    // let it through by. Two copies of the number could drift apart, and the guard would then be judging traffic
    // against a bound its sender never used.
    @Unique
    private static final double PARTICLE_RANGE = PacketReach.PARTICLE.blocks();

    @Unique
    private static final double OVERRIDDEN_PARTICLE_RANGE = PacketReach.FORCED_PARTICLE.blocks();

    @Unique
    private static final double BLOCK_DESTRUCTION_RANGE = 32.0;

    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    // The two tick containers are built in the level's own field initialisers, the raid registry comes out of saved
    // data, and the entity manager is handed nothing but its own storage — none of them ever learn which level owns
    // them, so they are handed it here, the first moment there is a level to hand.
    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindLevelToTickContainers(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        ((LevelBindable) level.getBlockTicks()).toroidal$bindLevel(level);
        ((LevelBindable) level.getFluidTicks()).toroidal$bindLevel(level);
        ((LevelBindable) level.getRaids()).toroidal$bindLevel(level);
        ((LevelBindable) this.entityManager).toroidal$bindLevel(level);
    }

    // Whatever moved an entity, at the end of its tick it is back inside the world — the same guarantee the player gets,
    // and for the same reason: a step past the boundary lands in a chunk that was never generated, and the entity is
    // simply lost. A lone on-foot player is left out on purpose: their own wrap also realigns the movement bounds vanilla
    // measures the next packet against.
    //
    // A wrap moves the entity a whole world, so the entity tracker sees a jump too large for a delta and sends a
    // teleport instead — which is translated on the way out, and the client sees the entity where it belongs.
    //
    // Passengers must move with the vehicle, by the same shift, in the same tick. Leave a passenger behind for even one
    // tick and it snaps back across the seam — the vehicle is dragged after it, and the two spend a moment a world apart:
    // a boat with a rider oscillates on the seam, and a mob in a minecart flickers as it crosses.
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

    // "Does this chunk tick?" is asked of coordinates the asker walked to, and a neighbourhood walked around a chunk at
    // the edge of the world names chunks past it — which are in no ticking range, having never been anywhere at all. The
    // question is answered about the chunk that physically exists, so a neighbourhood spanning the seam reads as the
    // continuous ground it is. A sculk sensor is the loudest casualty: it requires all nine chunks around it to tick, and
    // one block from the bounds it silently dropped every vibration it was ever sent.
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

    // The one server-side sink for the world spawn: /setworldspawn goes through here, a gametest through its own level,
    // and both end in front of level.dat and the packet that tells every client where the compass points. Settled here
    // rather than in the command, because the coordinate that reaches the command is not the only way in —
    // /setworldspawn without an argument reads the sender's position off the command source, so a sender standing past
    // the bounds after an /execute positioned would slip by a guard placed on the argument.
    //
    // Ahead of vanilla's own "did it change" comparison rather than behind it: the stored point is what the comparison
    // should be made against, or a spawn already at the folded coordinate would broadcast a packet saying nothing.
    @ModifyVariable(method = "setDefaultSpawnPos(Lnet/minecraft/core/BlockPos;F)V", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$storeWorldSpawnInsideBounds(BlockPos spawnPos) {
        return SeamRespawnData.insideBounds((ServerLevel) (Object) this, ReshapeProbe.WORLD_SPAWN, spawnPos);
    }

    // Vanilla-body re-implementation — verified against 26.2; re-diff on a platform bump.
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

    // The crack overlay a breaker draws on a block is offered to everyone within 32 blocks of it, and like the global
    // event above this walks the player list itself instead of going through the seam-aware PlayerList.broadcast. The
    // range is read in raw coordinates, so a witness standing a few steps away across the seam measures a whole world
    // and is told nothing: the block shatters with no warning and no animation, and the -1 that clears the overlay
    // never arrives either. Both server-side breakers reach here — a player mining and a zombie working a door — so
    // the gate is folded once, where they meet.
    //
    // The position stays canonical: ClientboundBlockDestructionPacket is translated into each client's own frame on the
    // way out. Vanilla's reading is kept exactly — the block's corner against the viewer's feet, its 32 blocks, and its
    // exclusion of the breaker, whose own client draws the crack locally.
    //
    // Vanilla-body re-implementation — verified against 26.2; re-diff on a platform bump.
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
}
