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
    private static final double GLOBAL_EVENT_RANGE = 32.0;

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

    // The border is per-level saved data and never learns which level owns it — it is handed two bare doubles for a
    // centre and measures everything against them. This is the one place the two are in the same room, so the level's
    // shape is stamped on here, and WorldBorderMixin folds its measurements against it.
    //
    // At the return rather than at a creation hook: the border is computed lazily out of the data storage, so there is
    // no construction to inject into. The identity check makes every call after the first a single reference compare,
    // and a level that does not wrap never writes at all — its transformer is the same NOOP the field starts on.
    @Inject(method = "getWorldBorder", at = @At("RETURN"))
    private void toroidal$bindBorderToLevelShape(CallbackInfoReturnable<WorldBorder> cir) {
        WorldLoopTransformer transformer = WorldLoopAttachments.transformerOf((ServerLevel) (Object) this);
        TransformerHolder border = (TransformerHolder) cir.getReturnValue();
        if (border.toroidal$transformer() != transformer) {
            border.toroidal$setTransformer(transformer);
        }
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

    // Spawning a player blocks the server thread until every chunk of a square around the spawn has its entities loaded.
    // The square is walked in raw coordinates, so a spawn near the bounds waits on chunks past them — and a chunk past
    // the bounds holds nothing and never will. It used to pass only because the phantom halo reached FULL there, which
    // is the cost this card removes: the wait was being paid for by chunks nobody could ever stand in.
    //
    // Entities live in the physical chunk, so that is what the wait must name. The square is restated in the frame of
    // the chunks that hold them, and folds to fewer positions in a world narrower than itself.
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

        return ChunkPos.pack(transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
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

    // A global event — a wither waking, a dragon dying, the end portal opening — is heard by every player in the world,
    // aimed at each of them separately instead of through PlayerList.broadcast. Vanilla gives the listener the event
    // where it happened while it is within 32 blocks, and past that pins it 32 blocks away along the direction to it, so
    // that a thousand-block-distant event still arrives from the right side. Both readings are taken in raw coordinates:
    // across the seam the event is a whole world away, so the near case can never win, and the direction the sound is
    // pinned along runs the long way round — which is the exact opposite side of the listener.
    //
    // The event is folded to the copy of itself nearest the listener before either reading is taken, so the distance is
    // the walk that exists and the direction points where the event really is. Vanilla's 32 blocks stand: the clamp is
    // what keeps the position within the listener's own reach, and sending the event's true coordinates instead would
    // hand the packet translation a point thousands of blocks past anything the client holds.
    //
    // The whole method is restated rather than injected into because vanilla does this work inside a lambda, whose
    // synthetic name is not something a mixin should be pinned to. With the game rule off, or on a world that does not
    // loop, nothing here applies and vanilla runs untouched — its own else-branch broadcasts through the seam-aware
    // PlayerList.broadcast already.
    //
    // Vanilla-body re-implementation — verified against 26.2; re-diff on a platform bump.
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

    // The level's own precipitation work, bound as a whole. Ice and snow do not stop being written once a chunk is
    // generated: tickChunk asks this of a random column on every loaded chunk, roughly once every 16 ticks at the
    // default random-tick speed, and the freeze branch is not even gated on the weather.
    //
    // Two of the three questions in vanilla's body go through Biome.shouldFreeze and shouldSnow, which bind for
    // themselves from the level they are handed. The third asks getPrecipitationAt straight off the biome — and that
    // one is handed no level at all, so nothing below this point has anything to bind from. A boundary binding and a
    // primitive binding are layers, not copies: this one says the level is doing precipitation work, the primitive
    // says it was asked. Drop this and the block that handles the precipitation — a cauldron filling at the seam —
    // silently reads the unfolded field again.
    @WrapMethod(method = "tickPrecipitation")
    private void toroidal$bindPrecipitationTransformer(BlockPos pos, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        GenerationTransformerContext.runWithTransformer(
                WorldLoopAttachments.transformerOf(level), () -> original.call(pos));
    }
}
