package com.toroidalworld.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.mixin.ChunkWaypointAccessor;
import com.toroidalworld.mixin.LevelChunkPacketAccessor;
import com.toroidalworld.mixin.LightUpdatePacketAccessor;
import com.toroidalworld.mixin.PlayerLookAtPacketAccessor;
import com.toroidalworld.mixin.Vec3iWaypointAccessor;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.player.ClientPosition.BorderCenter;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Every packet that carries a position crosses the boundary between the server's wrapped world and the client's
// unbounded one, so it is rewritten in flight. Packets not in the tables pass through untouched.
//
// The two directions are not symmetric. Outgoing, a world coordinate is unwrapped around where the player believes they
// are — which of the infinitely many copies of a chunk to show them depends on where they stand. Incoming, a client
// coordinate is simply wrapped back into the world: it names exactly one block, whichever copy they clicked.
//
// Rewriters read the world only through the TranslationContext, never a live player — the ServerPlayer entry points
// below resolve one, a test builds one by hand.
public final class PacketTranslator {
    // The two border packets both open with the centre as a pair of plain doubles, which is what lets the position in
    // front of them be swapped without naming any of their private fields.
    private static final StreamCodec<FriendlyByteBuf, BorderCenter> BORDER_CENTER_CODEC = StreamCodec.of(
            (buffer, center) -> {
                buffer.writeDouble(center.x());
                buffer.writeDouble(center.z());
            },
            buffer -> new BorderCenter(buffer.readDouble(), buffer.readDouble()));

    private static final Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> TO_CLIENT = Map.ofEntries(
            Map.entry(ClientboundLevelChunkWithLightPacket.class, rewriter(PacketTranslator::levelChunk)),
            Map.entry(ClientboundLightUpdatePacket.class, rewriter(PacketTranslator::lightUpdate)),
            Map.entry(ClientboundForgetLevelChunkPacket.class, rewriter(PacketTranslator::forgetChunk)),
            Map.entry(ClientboundSetChunkCacheCenterPacket.class, rewriter(PacketTranslator::chunkCacheCenter)),
            Map.entry(ClientboundChunksBiomesPacket.class, rewriter(PacketTranslator::chunkBiomes)),
            Map.entry(ClientboundPlayerPositionPacket.class, rewriter(PacketTranslator::playerPosition)),
            Map.entry(ClientboundBlockUpdatePacket.class, rewriter(PacketTranslator::blockUpdate)),
            Map.entry(ClientboundSectionBlocksUpdatePacket.class, rewriter(PacketTranslator::sectionBlocksUpdate)),
            Map.entry(ClientboundBlockEntityDataPacket.class, rewriter(PacketTranslator::blockEntityData)),
            Map.entry(ClientboundBlockDestructionPacket.class, rewriter(PacketTranslator::blockDestruction)),
            Map.entry(ClientboundSetEntityDataPacket.class, rewriter(PacketTranslator::setEntityData)),
            Map.entry(ClientboundAddEntityPacket.class, rewriter(PacketTranslator::addEntity)),
            Map.entry(ClientboundTeleportEntityPacket.class, rewriter(PacketTranslator::teleportEntity)),
            Map.entry(ClientboundEntityPositionSyncPacket.class, rewriter(PacketTranslator::entityPositionSync)),
            Map.entry(ClientboundMoveVehiclePacket.class, rewriter(PacketTranslator::moveVehicle)),
            Map.entry(ClientboundBlockEventPacket.class, rewriter(PacketTranslator::blockEvent)),
            Map.entry(ClientboundOpenSignEditorPacket.class, rewriter(PacketTranslator::openSignEditor)),
            Map.entry(ClientboundLevelEventPacket.class, rewriter(PacketTranslator::levelEvent)),
            Map.entry(ClientboundSoundPacket.class, rewriter(PacketTranslator::sound)),
            Map.entry(ClientboundLevelParticlesPacket.class, rewriter(PacketTranslator::levelParticles)),
            Map.entry(ClientboundExplodePacket.class, rewriter(PacketTranslator::explode)),
            Map.entry(ClientboundTrackedWaypointPacket.class, rewriter(PacketTranslator::trackedWaypoint)),
            Map.entry(ClientboundSetDefaultSpawnPositionPacket.class, rewriter(PacketTranslator::setDefaultSpawnPosition)),
            Map.entry(ClientboundInitializeBorderPacket.class, rewriter(PacketTranslator::initializeBorder)),
            Map.entry(ClientboundSetBorderCenterPacket.class, rewriter(PacketTranslator::setBorderCenter)),
            Map.entry(ClientboundPlayerLookAtPacket.class, rewriter(PacketTranslator::playerLookAt)),
            Map.entry(ClientboundDamageEventPacket.class, rewriter(PacketTranslator::damageEvent)),
            Map.entry(ClientboundMoveMinecartPacket.class, rewriter(PacketTranslator::moveMinecart)),
            Map.entry(ClientboundCustomPayloadPacket.class, rewriter(PacketTranslator::customPayload)));

    // Filled once by the loader glue while the mod initializes — before any server exists — then only read.
    private static final Map<Class<?>, BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>> PAYLOAD_REWRITERS =
            new HashMap<>();

    public static <P extends CustomPacketPayload> void registerPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        PAYLOAD_REWRITERS.put(payloadType,
                (payload, context) -> payloadRewriter.apply(payloadType.cast(payload), context));
    }

    // The particle twin of the payload table, with one extra input: a particle's own position folds around the origin
    // its packet was just translated to.
    public interface ParticleRewriter<P extends ParticleOptions> {
        ParticleOptions rewrite(P particle, TranslationContext context, Vec3 clientOrigin);
    }

    private static final Map<Class<?>, ParticleRewriter<ParticleOptions>> PARTICLE_REWRITERS = new HashMap<>();

    public static <P extends ParticleOptions> void registerParticleRewriter(Class<P> particleType,
            ParticleRewriter<P> particleRewriter) {
        PARTICLE_REWRITERS.put(particleType,
                (particle, context, clientOrigin) -> particleRewriter.rewrite(particleType.cast(particle), context, clientOrigin));
    }

    private static @Nullable ParticleRewriter<ParticleOptions> particleRewriterFor(ParticleOptions particle) {
        return PARTICLE_REWRITERS.get(particle.getClass());
    }

    private static final Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> TO_SERVER = Map.ofEntries(
            Map.entry(ServerboundUseItemOnPacket.class, rewriter(PacketTranslator::useItemOn)),
            Map.entry(ServerboundPlayerActionPacket.class, rewriter(PacketTranslator::playerAction)),
            Map.entry(ServerboundPickItemFromBlockPacket.class, rewriter(PacketTranslator::pickItemFromBlock)),
            Map.entry(ServerboundSignUpdatePacket.class, rewriter(PacketTranslator::signUpdate)),
            Map.entry(ServerboundBlockEntityTagQueryPacket.class, rewriter(PacketTranslator::blockEntityTagQuery)),
            Map.entry(ServerboundInteractPacket.class, rewriter(PacketTranslator::interact)),
            Map.entry(ServerboundJigsawGeneratePacket.class, rewriter(PacketTranslator::jigsawGenerate)),
            Map.entry(ServerboundSetCommandBlockPacket.class, rewriter(PacketTranslator::setCommandBlock)),
            Map.entry(ServerboundSetJigsawBlockPacket.class, rewriter(PacketTranslator::setJigsawBlock)),
            Map.entry(ServerboundSetStructureBlockPacket.class, rewriter(PacketTranslator::setStructureBlock)),
            Map.entry(ServerboundSetTestBlockPacket.class, rewriter(PacketTranslator::setTestBlock)),
            Map.entry(ServerboundTestInstanceBlockActionPacket.class, rewriter(PacketTranslator::testInstanceBlockAction)));

    // The context costs a record plus three player-capturing lambdas, and most traffic (entity moves, keepalives)
    // never hits the dispatch map — so the map is consulted first and the context built only for a packet that will
    // actually be rewritten, with the transformer the wrap guard already fetched passed along.
    public static <T extends net.minecraft.network.PacketListener> Packet<T> toClient(Packet<T> packet, ServerPlayer player) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return packet;
        }

        if (packet instanceof ClientboundBundlePacket bundle) {
            return castPacket(toClientBundle(bundle, player, transformer));
        }

        return dispatch(TO_CLIENT, packet, player, transformer);
    }

    static <T extends net.minecraft.network.PacketListener> Packet<T> toClient(Packet<T> packet, TranslationContext context) {
        return castPacket(rewrite(TO_CLIENT, packet, context));
    }

    // NeoForge ships every chunk inside a bundle (the chunk packet plus its auxiliary light payload), so the
    // contents have to be translated one by one. A sub-packet translated to null is a deliberate drop and has to be
    // skipped — a null element would NPE when the bundle serializes. One context serves every sub, built on the
    // first one the dispatch map knows. A rewriter that worked in place returns the same instance, so identity is
    // the change signal: when nothing was replaced or dropped the original bundle goes out as-is, already carrying
    // any in-place mutations.
    @SuppressWarnings("unchecked")
    private static Packet<?> toClientBundle(ClientboundBundlePacket bundle, ServerPlayer player, WorldLoopTransformer transformer) {
        TranslationContext context = null;
        List<Packet<? super ClientGamePacketListener>> translated = new ArrayList<>();
        boolean changed = false;
        for (Packet<? super ClientGamePacketListener> sub : bundle.subPackets()) {
            BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = TO_CLIENT.get(sub.getClass());
            if (rewriter == null) {
                translated.add(sub);
                continue;
            }

            if (context == null) {
                context = TranslationContext.of(player, transformer);
            }

            Packet<? super ClientGamePacketListener> translatedSub =
                    (Packet<? super ClientGamePacketListener>) rewriter.apply(sub, context);
            changed |= translatedSub != sub;
            if (translatedSub != null) {
                translated.add(translatedSub);
            }
        }

        return changed ? new ClientboundBundlePacket(translated) : bundle;
    }

    public static <T extends net.minecraft.network.PacketListener> Packet<T> toServer(Packet<T> packet, ServerPlayer player) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return packet;
        }

        return dispatch(TO_SERVER, packet, player, transformer);
    }

    static <T extends net.minecraft.network.PacketListener> Packet<T> toServer(Packet<T> packet, TranslationContext context) {
        return castPacket(rewrite(TO_SERVER, packet, context));
    }

    private static <T extends net.minecraft.network.PacketListener> Packet<T> dispatch(
            Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> rewriters,
            Packet<T> packet, ServerPlayer player, WorldLoopTransformer transformer) {
        BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = rewriters.get(packet.getClass());
        return rewriter == null ? packet
                : castPacket(rewriter.apply(packet, TranslationContext.of(player, transformer)));
    }

    private static Packet<?> rewrite(Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> rewriters,
            Packet<?> packet, TranslationContext context) {
        BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = rewriters.get(packet.getClass());
        return rewriter == null ? packet : rewriter.apply(packet, context);
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.network.PacketListener> Packet<T> castPacket(Packet<?> packet) {
        return (Packet<T>) packet;
    }

    // Chunk data and light data are opaque, position-independent blobs — only the two header ints name the chunk, so
    // the header is swapped in place instead of re-encoding hundreds of kilobytes per chunk. Mutating is safe because
    // both packets are built fresh for every recipient: the chunk packet in PlayerChunkSender.sendChunk, the light
    // packet per player via ChunkHolderMixin splitting vanilla's shared broadcast.
    private static ClientboundLevelChunkWithLightPacket levelChunk(ClientboundLevelChunkWithLightPacket packet, TranslationContext context) {
        ChunkPos clientPos = context.toClient(new ChunkPos(packet.getX(), packet.getZ()));

        LevelChunkPacketAccessor accessor = (LevelChunkPacketAccessor) packet;
        accessor.toroidal$setX(clientPos.x());
        accessor.toroidal$setZ(clientPos.z());
        return packet;
    }

    private static ClientboundLightUpdatePacket lightUpdate(ClientboundLightUpdatePacket packet, TranslationContext context) {
        ChunkPos clientPos = context.toClient(new ChunkPos(packet.getX(), packet.getZ()));

        LightUpdatePacketAccessor accessor = (LightUpdatePacketAccessor) packet;
        accessor.toroidal$setX(clientPos.x());
        accessor.toroidal$setZ(clientPos.z());
        return packet;
    }

    // A forget arrives with the raw view coordinate, decided against the old view centre but sent after the mirror
    // has already moved on — on a multi-chunk view jump (speed, teleport) the fold can flip to the copy the client
    // does not hold, leaving a ghost chunk rendered behind the player. When the coordinate lands past the view's
    // reach the choice is not trusted: a forget goes out for every copy the client might hold, and the unheld ones
    // are client-side no-ops.
    private static Packet<?> forgetChunk(ClientboundForgetLevelChunkPacket packet, TranslationContext context) {
        List<ChunkPos> candidates = context.forgetCandidates(packet.pos());
        if (candidates.size() == 1) {
            return new ClientboundForgetLevelChunkPacket(candidates.getFirst());
        }

        List<Packet<? super ClientGamePacketListener>> forgets = new ArrayList<>(candidates.size());
        for (ChunkPos candidate : candidates) {
            forgets.add(new ClientboundForgetLevelChunkPacket(candidate));
        }

        return new ClientboundBundlePacket(forgets);
    }

    // The one packet that moves the anchor every other chunk packet is folded and judged against, so it takes the
    // door's own entry point: folded around the mirror and outside the view-reach check.
    private static ClientboundSetChunkCacheCenterPacket chunkCacheCenter(ClientboundSetChunkCacheCenterPacket packet, TranslationContext context) {
        ChunkPos clientPos = context.toClientCacheCenter(new ChunkPos(packet.getX(), packet.getZ()));
        return new ClientboundSetChunkCacheCenterPacket(clientPos.x(), clientPos.z());
    }

    // A custom payload's shape is its owner's business — a loader ships payloads of its own (NeoForge's auxiliary
    // light data), and their classes are loader API this table must not name. So the packet dispatches to a second,
    // payload-keyed table that the loader glue fills at init; a payload nobody claimed passes through untouched.
    private static Packet<?> customPayload(ClientboundCustomPayloadPacket packet, TranslationContext context) {
        BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> payloadRewriter =
                PAYLOAD_REWRITERS.get(packet.payload().getClass());
        if (payloadRewriter == null) {
            return packet;
        }

        CustomPacketPayload rewritten = payloadRewriter.apply(packet.payload(), context);
        return rewritten == packet.payload() ? packet : new ClientboundCustomPayloadPacket(rewritten);
    }

    // The client's own position. A relative move is a delta the client applies to itself, so it already lands in the
    // right space and only the mirror has to follow; an absolute one is a server coordinate and has to be moved into
    // the client's space, or the client would be flung back a whole world and take its chunk cache with it.
    //
    // That move takes the plain nearest-copy door. A teleport names any point in the world it likes, the antipode
    // included, and every one of them is a legal target; the guarded door's band is there for coordinates that only
    // reach the client because the player is near them, which this is not. The step the move makes is still checked —
    // by the mirror itself, one line below, where both ends of it are known.
    //
    // This is where an arrival in another dimension is noticed, and it is the right place for it because every absolute
    // position a player is given on the way into a new world passes through here — portal, command, login — before any
    // packet that depends on the mirror. An event would fire after them.
    //
    // It does not cover a move the server makes without changing dimension: a respawn lands in the same world, so the
    // mirror still describes it and the branch below does not fire. That case is caught at the placement itself, in
    // EntityMixin's snapTo hook — the two together are what make every change of space seed the mirror.
    private static ClientboundPlayerPositionPacket playerPosition(ClientboundPlayerPositionPacket packet, TranslationContext context) {
        ClientPosition clientPosition = context.clientPosition();

        // A mirror built for another dimension names a place in a different world, so there is nothing to shift against:
        // it is rebased on where the player actually arrived, and the packet goes out unchanged, leaving both sides
        // agreeing at that moment. The position is read off the player rather than the packet because
        // ServerGamePacketListenerImpl.teleport applies the move — resolving any relative axes — before sending it.
        if (!clientPosition.describes(context.dimension())) {
            context.rebase().run();
            return packet;
        }

        PositionMoveRotation change = packet.change();
        Vec3 position = change.position();

        // A relative delta reaches the client as-is and moves its unbounded coordinate by that much. Folded to its
        // shortest equivalent through the seam it names the same physical arrival, but a lap-sized hop (a relative
        // move of about a world width) stops carrying the client a world over — where every chunk it holds would
        // re-anchor to a different copy and have to be re-sent.
        boolean relativeX = packet.relatives().contains(Relative.X);
        boolean relativeZ = packet.relatives().contains(Relative.Z);
        double foldedX = relativeX ? context.transformer().coords.x.foldDelta(position.x) : 0.0;
        double foldedZ = relativeZ ? context.transformer().coords.z.foldDelta(position.z) : 0.0;
        double clientX = relativeX ? clientPosition.x() + foldedX : context.nearestCopyX(position.x);
        double clientZ = relativeZ ? clientPosition.z() + foldedZ : context.nearestCopyZ(position.z);
        clientPosition.set(clientX, clientZ);

        Vec3 sentPosition = new Vec3(relativeX ? foldedX : clientX, position.y, relativeZ ? foldedZ : clientZ);
        return new ClientboundPlayerPositionPacket(
                packet.id(),
                new PositionMoveRotation(sentPosition, change.deltaMovement(), change.yRot(), change.xRot()),
                packet.relatives());
    }

    // Entities are placed by absolute position when they appear or are teleported; their ordinary movement travels as a
    // delta from the last known position and needs no translation.
    private static ClientboundAddEntityPacket addEntity(ClientboundAddEntityPacket packet, TranslationContext context) {
        PacketReach reach = context.trackedReach();
        return new ClientboundAddEntityPacket(
                packet.getId(), packet.getUUID(),
                context.toClientX(packet.getX(), reach),
                packet.getY(),
                context.toClientZ(packet.getZ(), reach),
                packet.getXRot(), packet.getYRot(), packet.getType(), packet.getData(),
                packet.getMovement(), packet.getYHeadRot());
    }

    // The rider predicts their own vehicle; a correction only arrives when the server disagrees, which the continuous
    // read prevents. Across the seam the server would still send one — its position jumped a world — and the client,
    // told to snap its own boat, resets interpolation and jolts. Dropping it leaves the smooth local prediction alone.
    // Only a vehicle the player actually steers is dropped: a minecart the server drives must keep being sent, or it
    // would freeze on the passenger's screen.
    private static Packet<?> teleportEntity(ClientboundTeleportEntityPacket packet, TranslationContext context) {
        if (context.ownVehicle().test(packet.id())) {
            return null;
        }

        return new ClientboundTeleportEntityPacket(
                packet.id(), toClientChange(context, packet.change(), packet.relatives()), packet.relatives(),
                packet.onGround());
    }

    private static Packet<?> entityPositionSync(ClientboundEntityPositionSyncPacket packet, TranslationContext context) {
        if (context.ownVehicle().test(packet.id())) {
            return null;
        }

        return new ClientboundEntityPositionSyncPacket(
                packet.id(), toClientChange(context, packet.values(), Set.of()), packet.onGround());
    }

    // The correction only ever names the vehicle the recipient is riding, so it arrives from no distance at all; the
    // tracking reach is a generous bound rather than a tight one, and the tight one would be zero.
    private static Packet<?> moveVehicle(ClientboundMoveVehiclePacket packet, TranslationContext context) {
        Vec3 clientPos = context.toClient(packet.position(), context.trackedReach());
        return new ClientboundMoveVehiclePacket(clientPos, packet.yRot(), packet.xRot());
    }

    // A relative move is a delta the client applies itself, so it already lands in the right space; an absolute one is a
    // server coordinate and has to be moved into the client's.
    private static PositionMoveRotation toClientChange(TranslationContext context, PositionMoveRotation change, Set<Relative> relatives) {
        PacketReach reach = context.trackedReach();
        Vec3 position = change.position();
        double clientX = relatives.contains(Relative.X) ? position.x : context.toClientX(position.x, reach);
        double clientZ = relatives.contains(Relative.Z) ? position.z : context.toClientZ(position.z, reach);
        return new PositionMoveRotation(
                new Vec3(clientX, position.y, clientZ), change.deltaMovement(), change.yRot(), change.xRot());
    }

    private static ClientboundBlockUpdatePacket blockUpdate(ClientboundBlockUpdatePacket packet, TranslationContext context) {
        return new ClientboundBlockUpdatePacket(toClientBlock(context, packet.getPos()), packet.getBlockState());
    }

    // Several blocks changing in one section travel as a batch, and the section they belong to sits in a private field.
    private static ClientboundSectionBlocksUpdatePacket sectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, TranslationContext context) {
        return rewritePosition(ClientboundSectionBlocksUpdatePacket.STREAM_CODEC, SectionPos.STREAM_CODEC, packet, context,
                section -> SectionPos.of(context.toClient(section.chunk()), section.y()));
    }

    private static ClientboundBlockEntityDataPacket blockEntityData(ClientboundBlockEntityDataPacket packet, TranslationContext context) {
        return rewritePosition(ClientboundBlockEntityDataPacket.STREAM_CODEC, BlockPos.STREAM_CODEC, packet, context,
                pos -> toClientBlock(context, pos));
    }

    private static ClientboundBlockDestructionPacket blockDestruction(ClientboundBlockDestructionPacket packet, TranslationContext context) {
        return new ClientboundBlockDestructionPacket(
                packet.getId(), toClientBlock(context, packet.getPos()), packet.getProgress());
    }

    private static ClientboundLevelEventPacket levelEvent(ClientboundLevelEventPacket packet, TranslationContext context) {
        return new ClientboundLevelEventPacket(
                packet.getType(), toClientBlock(context, packet.getPos()), packet.getData(), packet.isGlobalEvent());
    }

    // Entity data carries positions with their type erased — the bed a player is sleeping in travels as an anonymous
    // value inside a list. Left untranslated, the client puts itself in the server's copy of the bed: a whole world from
    // where it stands, taking its chunk cache with it. A plain BlockPos travels the same way (a falling block's start
    // position — the seed of its model variant), so both shapes are moved.
    //
    // A particle payload is the third shape, and the only one whose anchor is not in the packet: an area effect cloud
    // sprays its payload around itself, so the position inside it belongs beside the entity, not beside the player. The
    // packet names only an id, so the entity is resolved and moved into client space — the very value the add and the
    // position-sync rewriters would compute in the same breath, which is what keeps payload and cloud in one copy. It
    // is resolved once for the packet and only when something in it actually carries a payload.
    private static ClientboundSetEntityDataPacket setEntityData(ClientboundSetEntityDataPacket packet, TranslationContext context) {
        List<SynchedEntityData.DataValue<?>> items = packet.packedItems();
        Vec3 anchor = carriesParticle(items) ? entityAnchor(packet.id(), context) : null;

        List<SynchedEntityData.DataValue<?>> translated = new ArrayList<>(items.size());
        for (SynchedEntityData.DataValue<?> item : items) {
            translated.add(toClientData(item, anchor, context));
        }

        return new ClientboundSetEntityDataPacket(packet.id(), translated);
    }

    private static boolean carriesParticle(List<SynchedEntityData.DataValue<?>> items) {
        for (SynchedEntityData.DataValue<?> item : items) {
            if (item.value() instanceof ParticleOptions
                    || item.value() instanceof List<?> values && isParticleList(values)) {
                return true;
            }
        }

        return false;
    }

    // A payload can also arrive as a list — the effect particles a mob shows. Vanilla builds those through MobEffect's
    // default factory, which carries no position, but the constructors take any ParticleOptions and the value reaches
    // the wire with its type erased just the same. Every element is checked rather than the first: nothing about a list
    // in entity data promises it holds particles at all.
    private static boolean isParticleList(List<?> values) {
        if (values.isEmpty()) {
            return false;
        }

        for (Object value : values) {
            if (!(value instanceof ParticleOptions)) {
                return false;
            }
        }

        return true;
    }

    // Without the entity there is nothing to fold the payload around — it has despawned mid-flight, and the packet
    // describes something the client is about to drop anyway, so the payload travels as it came.
    private static @Nullable Vec3 entityAnchor(int entityId, TranslationContext context) {
        Vec3 serverPosition = context.entityPosition().apply(entityId);
        return serverPosition == null ? null : context.toClient(serverPosition, context.trackedReach());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SynchedEntityData.DataValue<?> toClientData(SynchedEntityData.DataValue<?> item,
            @Nullable Vec3 anchor, TranslationContext context) {
        if (item.value() instanceof BlockPos pos) {
            return new SynchedEntityData.DataValue(item.id(), item.serializer(), toClientBlock(context, pos));
        }

        if (item.value() instanceof Optional<?> optional && optional.orElse(null) instanceof BlockPos pos) {
            return new SynchedEntityData.DataValue(item.id(), item.serializer(), Optional.of(toClientBlock(context, pos)));
        }

        if (anchor != null && item.value() instanceof ParticleOptions particle) {
            ParticleOptions clientParticle = toClientParticle(context, particle, anchor);
            return clientParticle == particle ? item
                    : new SynchedEntityData.DataValue(item.id(), item.serializer(), clientParticle);
        }

        if (anchor != null && item.value() instanceof List<?> values && isParticleList(values)) {
            List<ParticleOptions> clientParticles = new ArrayList<>(values.size());
            boolean changed = false;
            for (Object value : values) {
                ParticleOptions particle = (ParticleOptions) value;
                ParticleOptions clientParticle = toClientParticle(context, particle, anchor);
                changed |= clientParticle != particle;
                clientParticles.add(clientParticle);
            }

            return changed ? new SynchedEntityData.DataValue(item.id(), item.serializer(), clientParticles) : item;
        }

        return item;
    }

    private static ClientboundBlockEventPacket blockEvent(ClientboundBlockEventPacket packet, TranslationContext context) {
        return new ClientboundBlockEventPacket(
                toClientBlock(context, packet.getPos()), packet.getBlock(), packet.getB0(), packet.getB1());
    }

    // The editor the client opens must name the sign in its own space, or the text it sends back would name a block a
    // world away from the one it is showing.
    private static ClientboundOpenSignEditorPacket openSignEditor(ClientboundOpenSignEditorPacket packet, TranslationContext context) {
        return new ClientboundOpenSignEditorPacket(toClientBlock(context, packet.getPos()), packet.isFrontText());
    }

    // Sounds, particles and explosions are not anchored to a chunk, so they are unwrapped around the player: of all the
    // copies of that spot in the world, the one nearest them is the one they should hear and see.
    //
    // Each also carries its own radius on the wire, which is what the guarded door is held to. A sound's is the very
    // number its sender gated on — the packet keeps both the event and the volume getRange was computed from — so the
    // guard asks the sound itself how far it was meant to travel.
    private static ClientboundSoundPacket sound(ClientboundSoundPacket packet, TranslationContext context) {
        PacketReach reach = PacketReach.sound(packet.getSound().value().getRange(packet.getVolume()));
        return new ClientboundSoundPacket(
                packet.getSound(), packet.getSource(),
                context.toClientX(packet.getX(), reach),
                packet.getY(),
                context.toClientZ(packet.getZ(), reach),
                packet.getVolume(), packet.getPitch(), packet.getSeed());
    }

    // The flag that widens a particle's reach from 32 blocks to 512 is the same one the sender gated on, and it rides
    // along on the packet — so /particle with force names its own far bound instead of tripping a narrow one.
    private static ClientboundLevelParticlesPacket levelParticles(ClientboundLevelParticlesPacket packet, TranslationContext context) {
        PacketReach reach = packet.isOverrideLimiter() ? PacketReach.FORCED_PARTICLE : PacketReach.PARTICLE;
        Vec3 clientOrigin = context.toClient(new Vec3(packet.getX(), packet.getY(), packet.getZ()), reach);
        return new ClientboundLevelParticlesPacket(
                toClientParticle(context, packet.getParticle(), clientOrigin),
                packet.isOverrideLimiter(), packet.alwaysShow(),
                clientOrigin.x, clientOrigin.y, clientOrigin.z,
                packet.getXDist(), packet.getYDist(), packet.getZDist(), packet.getMaxSpeed(), packet.getCount());
    }

    // Both the burst particle and the block particles are spawned around the centre client-side, so the centre is the
    // anchor for anything they carry inside.
    private static ClientboundExplodePacket explode(ClientboundExplodePacket packet, TranslationContext context) {
        Vec3 clientCenter = context.toClient(packet.center(), PacketReach.EXPLOSION);
        return new ClientboundExplodePacket(
                clientCenter, packet.radius(), packet.blockCount(), packet.playerKnockback(),
                toClientParticle(context, packet.explosionParticle(), clientCenter),
                packet.explosionSound(),
                toClientBlockParticles(context, packet.blockParticles(), clientCenter));
    }

    // Three particle payloads carry a second, absolute position of their own, and the packet coordinate they ride on has
    // just been moved a whole world; left as they came, the particle is drawn from a translated start toward a raw
    // server coordinate. Each is folded to the copy nearest that start — a target the client can actually fly to.
    //
    // Which fold differs by what the position names. A trail target and a vibration's sensor are loose points a few
    // blocks from the start and take the plain nearest copy. A block particle's position names a block whose model data
    // the client looks up, so it takes the chunk-anchored fold: it has to land in the copy of the chunk the client
    // holds, or the lookup resolves against nothing and falls back to the default sprite.
    //
    // A vibration travelling to a warden or an allay carries an entity position source, which is an entity id on the
    // wire and resolves to the client's own copy — already in client space, and nothing to move.
    private static ParticleOptions toClientParticle(TranslationContext context, ParticleOptions particle,
            Vec3 clientOrigin) {
        switch (particle) {
            case TrailParticleOption trail -> {
                return new TrailParticleOption(
                        context.transformer().vectors.nearestCopy(clientOrigin, trail.target()),
                        trail.color(), trail.duration());
            }
            case VibrationParticleOption vibration -> {
                if (!(vibration.getDestination() instanceof BlockPositionSource destination)) {
                    return particle;
                }

                BlockPos clientDestination = context.transformer().blocks.nearestCopy(
                        BlockPos.containing(clientOrigin), destination.pos());
                return new VibrationParticleOption(
                        new BlockPositionSource(clientDestination), vibration.getArrivalInTicks());
            }
            // A particle payload only a loader ships (NeoForge's block particle carries an extra position) is its
            // owner's to move — the loader glue contributes a rewriter; an unclaimed particle passes untouched.
            default -> {
                ParticleRewriter<ParticleOptions> particleRewriter = particleRewriterFor(particle);
                return particleRewriter == null ? particle : particleRewriter.rewrite(particle, context, clientOrigin);
            }
        }
    }

    // A payload with nothing to move comes back as the instance it went in as, so a list where none of them carried a
    // position is handed straight back — which is every explosion vanilla itself throws.
    private static WeightedList<ExplosionParticleInfo> toClientBlockParticles(TranslationContext context,
            WeightedList<ExplosionParticleInfo> blockParticles, Vec3 clientOrigin) {
        List<Weighted<ExplosionParticleInfo>> entries = blockParticles.unwrap();
        List<Weighted<ExplosionParticleInfo>> translated = new ArrayList<>(entries.size());
        boolean changed = false;
        for (Weighted<ExplosionParticleInfo> entry : entries) {
            ExplosionParticleInfo info = entry.value();
            ParticleOptions particle = toClientParticle(context, info.particle(), clientOrigin);
            changed |= particle != info.particle();
            translated.add(particle == info.particle() ? entry
                    : new Weighted<>(new ExplosionParticleInfo(particle, info.scaling(), info.speed()), entry.weight()));
        }

        return changed ? WeightedList.of(translated) : blockParticles;
    }

    // The locator bar's waypoints carry the source position in server coordinates; the client computes the arrow's
    // angle in its own unbounded space, so the position is moved to the copy the client holds — the same copy its
    // entities live in, which is also what lets the client anchor the waypoint to the visible player. A waypoint may
    // legitimately name a chunk far beyond the view — it is a directional hint, not a chunk the client holds — so it
    // takes the plain nearest-copy unwrap, outside the view-reach backstop. The waypoint is translated in place
    // rather than rebuilt: vanilla creates a fresh instance for every send, and a rebuild would have to name the
    // packet's private Operation enum. Azimuth and empty waypoints carry no coordinate and pass as-is.
    private static ClientboundTrackedWaypointPacket trackedWaypoint(ClientboundTrackedWaypointPacket packet, TranslationContext context) {
        if (packet.waypoint() instanceof Vec3iWaypointAccessor waypoint) {
            waypoint.toroidal$setVector(nearestCopyBlock(context, new BlockPos(waypoint.toroidal$getVector())));
        } else if (packet.waypoint() instanceof ChunkWaypointAccessor waypoint) {
            waypoint.toroidal$setChunkPos(context.nearestCopy(waypoint.toroidal$getChunkPos()));
        }

        return packet;
    }

    // The compass needle is computed in the client's unbounded space, so the spawn is moved to the copy nearest the
    // player — a directional hint that may legitimately sit far beyond the view, like a waypoint. The packet carries a
    // GlobalPos that may name another dimension's spawn; a foreign coordinate has nothing to do with this world's wrap
    // and passes as-is.
    private static Packet<?> setDefaultSpawnPosition(ClientboundSetDefaultSpawnPositionPacket packet, TranslationContext context) {
        LevelData.RespawnData respawnData = packet.respawnData();
        if (!respawnData.dimension().equals(context.dimension())) {
            return packet;
        }

        BlockPos clientPos = nearestCopyBlock(context, respawnData.pos());
        context.clientPosition().setHeldSpawn(clientPos);
        return new ClientboundSetDefaultSpawnPositionPacket(new LevelData.RespawnData(
                GlobalPos.of(respawnData.dimension(), clientPos),
                respawnData.yaw(), respawnData.pitch()));
    }

    // The world border's centre is the second absolute coordinate the client keeps for good, and vanilla is as sparing
    // with it as with the world spawn: once on the way into a level, then only when someone moves it. The client
    // measures its own copy of the border in the unbounded space it believes in, so a canonical centre puts the wall a
    // whole world from where the server measures it after a single lap — and the client refuses block breaking and
    // placement against that same wrong square, so it is more than a wall drawn in the wrong place.
    //
    // Only two of the six border packets carry a centre; size, lerp, warning delay and warning distance name no
    // coordinate and pass untouched. Neither of the two can be rebuilt — private fields, and the only public
    // constructor takes a live WorldBorder — but the centre is the first thing on the wire in both, two plain doubles,
    // so it is re-encoded in front of the untouched remainder.
    //
    // A mirror belonging to another dimension has nothing to fold against: the packet goes out as it came and the
    // stored copy stays empty, so the watcher sends a fresh one on the first tick after the rebase.
    private static Packet<?> initializeBorder(ClientboundInitializeBorderPacket packet, TranslationContext context) {
        if (!context.clientPosition().describes(context.dimension())) {
            return packet;
        }

        return rewritePosition(ClientboundInitializeBorderPacket.STREAM_CODEC, BORDER_CENTER_CODEC, packet, context,
                center -> toClientBorderCenter(context, center));
    }

    private static Packet<?> setBorderCenter(ClientboundSetBorderCenterPacket packet, TranslationContext context) {
        if (!context.clientPosition().describes(context.dimension())) {
            return packet;
        }

        return rewritePosition(ClientboundSetBorderCenterPacket.STREAM_CODEC, BORDER_CENTER_CODEC, packet, context,
                center -> toClientBorderCenter(context, center));
    }

    private static BorderCenter toClientBorderCenter(TranslationContext context, BorderCenter center) {
        ClientPosition clientPosition = context.clientPosition();
        BorderCenter clientCenter = nearestCopyCenter(context.transformer(), clientPosition, center);
        clientPosition.setHeldBorderCenter(clientCenter);
        return clientCenter;
    }

    // Also the ClientAnchorSync flip check — recomputed outside a packet flow, so the math is shared here and the
    // stored copy can never disagree with what a re-send would produce.
    //
    // The plain nearest-copy fold, the door nearestCopyX/Z open inside a packet flow: a border centre may legitimately
    // sit half a world from the player, which is the very distance the guarded door exists to shout about.
    static BorderCenter nearestCopyCenter(WorldLoopTransformer transformer, ClientPosition clientPosition,
            BorderCenter center) {
        return new BorderCenter(
                nearestCopyCenterX(transformer, clientPosition, center.x()),
                nearestCopyCenterZ(transformer, clientPosition, center.z()));
    }

    static double nearestCopyCenterX(WorldLoopTransformer transformer, ClientPosition clientPosition, double centerX) {
        return transformer.coords.x.unwrapAround(clientPosition.x(), centerX);
    }

    static double nearestCopyCenterZ(WorldLoopTransformer transformer, ClientPosition clientPosition, double centerZ) {
        return transformer.coords.z.unwrapAround(clientPosition.z(), centerZ);
    }

    // The client turns toward the target in its own unbounded space — moved to the nearest copy, the turn goes the
    // short way across the seam. The at-entity form resolves the live entity client-side, already in client space;
    // the coordinates riding along are its fallback and are moved the same way. The target hides in private fields
    // with no rebuild path, so the packet is moved in place — vanilla creates a fresh instance for every send.
    //
    // A turn is aimed at whatever point the command named, so the target may sit anywhere in the world — a directional
    // hint like a waypoint, not a coordinate the player's nearness put on the wire. It takes the plain nearest-copy
    // fold for the same reason a waypoint does.
    private static ClientboundPlayerLookAtPacket playerLookAt(ClientboundPlayerLookAtPacket packet, TranslationContext context) {
        PlayerLookAtPacketAccessor accessor = (PlayerLookAtPacketAccessor) packet;
        accessor.toroidal$setX(context.nearestCopyX(accessor.toroidal$getX()));
        accessor.toroidal$setZ(context.nearestCopyZ(accessor.toroidal$getZ()));
        return packet;
    }

    // The source position drives the directional damage tilt and the shield's block arc, both computed in client
    // space; nearest the player it points from the correct side of the seam. It is only present when the source has
    // no entity — an entity source resolves to the client's own copy of that entity. The packet goes to whoever is
    // tracking the entity that was hurt, and the one positional source vanilla has without an entity is the exploding
    // respawn anchor, which stands where the sleeper does — so the tracking reach bounds it.
    private static Packet<?> damageEvent(ClientboundDamageEventPacket packet, TranslationContext context) {
        if (packet.sourcePosition().isEmpty()) {
            return packet;
        }

        PacketReach reach = context.trackedReach();
        return new ClientboundDamageEventPacket(
                packet.entityId(), packet.sourceType(), packet.sourceCauseId(), packet.sourceDirectId(),
                packet.sourcePosition().map(position -> context.toClient(position, reach)));
    }

    // The minecart-improvements cart travels as a list of absolute lerp steps; each position is moved to the copy the
    // client holds, while the movement deltas and rotations ride along unchanged. Consecutive steps sit a fraction of
    // a block apart, so unwrapping each around the player keeps the chain continuous across the seam.
    private static ClientboundMoveMinecartPacket moveMinecart(ClientboundMoveMinecartPacket packet, TranslationContext context) {
        PacketReach reach = context.trackedReach();
        List<NewMinecartBehavior.MinecartStep> translated = new ArrayList<>(packet.lerpSteps().size());
        for (NewMinecartBehavior.MinecartStep step : packet.lerpSteps()) {
            translated.add(new NewMinecartBehavior.MinecartStep(
                    context.toClient(step.position(), reach), step.movement(), step.yRot(), step.xRot(), step.weight()));
        }

        return new ClientboundMoveMinecartPacket(packet.entityId(), translated);
    }

    private static ClientboundChunksBiomesPacket chunkBiomes(ClientboundChunksBiomesPacket packet, TranslationContext context) {
        return new ClientboundChunksBiomesPacket(packet.chunkBiomeData().stream()
                .map(data -> new ClientboundChunksBiomesPacket.ChunkBiomeData(
                        context.toClient(data.pos()), data.buffer()))
                .toList());
    }

    // The block the player clicked is named in their own space, and the point they hit travels with it — but it cannot be
    // wrapped on its own. A block owns [z, z+1), so a hit on its far face sits at exactly z+1, which already belongs to
    // the next block: wrapped separately, the point and the block land on opposite sides of the seam, and vanilla's
    // check that the hit lies inside the block it names quietly throws the packet away. The block is the anchor; the
    // offset within it is carried across unchanged.
    private static ServerboundUseItemOnPacket useItemOn(ServerboundUseItemOnPacket packet, TranslationContext context) {
        BlockHitResult hit = packet.getHitResult();
        BlockPos pos = context.toServer(hit.getBlockPos());
        Vec3 offsetInBlock = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        Vec3 location = Vec3.atLowerCornerOf(pos).add(offsetInBlock);

        BlockHitResult wrapped = hit.getType() == HitResult.Type.MISS
                ? BlockHitResult.miss(location, hit.getDirection(), pos)
                : new BlockHitResult(location, hit.getDirection(), pos, hit.isInside(), hit.isWorldBorderHit());
        return new ServerboundUseItemOnPacket(packet.getHand(), wrapped, packet.getSequence());
    }

    private static ServerboundPlayerActionPacket playerAction(ServerboundPlayerActionPacket packet, TranslationContext context) {
        return new ServerboundPlayerActionPacket(packet.getAction(), context.toServer(packet.getPos()),
                packet.getDirection(), packet.getSequence());
    }

    private static ServerboundPickItemFromBlockPacket pickItemFromBlock(ServerboundPickItemFromBlockPacket packet, TranslationContext context) {
        return new ServerboundPickItemFromBlockPacket(
                context.toServer(packet.pos()), packet.includeData());
    }

    private static ServerboundSignUpdatePacket signUpdate(ServerboundSignUpdatePacket packet, TranslationContext context) {
        String[] lines = packet.getLines();
        return new ServerboundSignUpdatePacket(context.toServer(packet.getPos()),
                packet.isFrontText(), lines[0], lines[1], lines[2], lines[3]);
    }

    private static ServerboundBlockEntityTagQueryPacket blockEntityTagQuery(ServerboundBlockEntityTagQueryPacket packet, TranslationContext context) {
        return new ServerboundBlockEntityTagQueryPacket(
                packet.getTransactionId(), context.toServer(packet.getPos()));
    }

    // The point the player hit on an entity must land beside the entity it names — the server reads the hit relative to
    // the entity's position. A plain wrap is not enough: for an entity standing on the seam the hit point can fall past
    // the world boundary, and wrapped on its own it lands a whole world from the entity. The location is folded to the
    // copy nearest the entity itself; without the entity (despawned mid-flight) the plain wrap is the best that's left.
    private static ServerboundInteractPacket interact(ServerboundInteractPacket packet, TranslationContext context) {
        Vec3 targetPosition = context.entityPosition().apply(packet.entityId());
        Vec3 location = packet.location();

        Vec3 serverLocation = targetPosition == null
                ? context.toServer(location)
                : context.transformer().vectors.nearestCopy(targetPosition, location);
        return new ServerboundInteractPacket(
                packet.entityId(), packet.hand(), serverLocation, packet.usingSecondaryAction());
    }

    // A block-entity screen sends back the position it was opened with — a client-frame coordinate, same as SignUpdate.
    private static ServerboundJigsawGeneratePacket jigsawGenerate(ServerboundJigsawGeneratePacket packet, TranslationContext context) {
        return new ServerboundJigsawGeneratePacket(
                context.toServer(packet.getPos()), packet.levels(), packet.keepJigsaws());
    }

    private static ServerboundSetCommandBlockPacket setCommandBlock(ServerboundSetCommandBlockPacket packet, TranslationContext context) {
        return new ServerboundSetCommandBlockPacket(
                context.toServer(packet.getPos()), packet.getCommand(), packet.getMode(),
                packet.isTrackOutput(), packet.isConditional(), packet.isAutomatic());
    }

    private static ServerboundSetJigsawBlockPacket setJigsawBlock(ServerboundSetJigsawBlockPacket packet, TranslationContext context) {
        return new ServerboundSetJigsawBlockPacket(
                context.toServer(packet.getPos()), packet.getName(), packet.getTarget(),
                packet.getPool(), packet.getFinalState(), packet.getJoint(),
                packet.getSelectionPriority(), packet.getPlacementPriority());
    }

    // Only the anchor position is wrapped — offset and size are relative byte deltas.
    private static ServerboundSetStructureBlockPacket setStructureBlock(ServerboundSetStructureBlockPacket packet, TranslationContext context) {
        return new ServerboundSetStructureBlockPacket(
                context.toServer(packet.getPos()), packet.getUpdateType(), packet.getMode(),
                packet.getName(), packet.getOffset(), packet.getSize(), packet.getMirror(), packet.getRotation(),
                packet.getData(), packet.isIgnoreEntities(), packet.isStrict(), packet.isShowAir(),
                packet.isShowBoundingBox(), packet.getIntegrity(), packet.getSeed());
    }

    private static ServerboundSetTestBlockPacket setTestBlock(ServerboundSetTestBlockPacket packet, TranslationContext context) {
        return new ServerboundSetTestBlockPacket(
                context.toServer(packet.position()), packet.mode(), packet.message());
    }

    private static ServerboundTestInstanceBlockActionPacket testInstanceBlockAction(ServerboundTestInstanceBlockActionPacket packet, TranslationContext context) {
        return new ServerboundTestInstanceBlockActionPacket(
                context.toServer(packet.pos()), packet.action(), packet.data());
    }

    // A block update has to land on the copy of the chunk the client is actually holding, which is the one it was sent
    // under — not the one this position would map to now that the player has moved.
    static BlockPos toClientBlock(TranslationContext context, BlockPos pos) {
        return blockInChunkCopy(context.toClient(ChunkPos.containing(pos)), pos);
    }

    // A far block waypoint is a directional hint, not a block in a held chunk — translated outside the view-reach
    // backstop, same as its chunk-shaped sibling.
    private static BlockPos nearestCopyBlock(TranslationContext context, BlockPos pos) {
        return nearestCopyBlock(context.transformer(), context.clientPosition().chunk(), pos);
    }

    // Also the ClientAnchorSync flip check — recomputed outside a packet flow, so the math is shared here and the
    // stored copy can never disagree with what a re-send would produce. The per-axis forms are the primitive truth:
    // the tick-side check reads them directly and builds nothing on its steady-state path.
    static BlockPos nearestCopyBlock(WorldLoopTransformer transformer, ChunkPos anchor, BlockPos pos) {
        return new BlockPos(
                nearestCopyBlockX(transformer, anchor.x(), pos.getX()),
                pos.getY(),
                nearestCopyBlockZ(transformer, anchor.z(), pos.getZ()));
    }

    static int nearestCopyBlockX(WorldLoopTransformer transformer, int anchorChunkX, int blockX) {
        int nearestChunkX = transformer.chunks.x.unwrap(anchorChunkX, SectionPos.blockToSectionCoord(blockX));
        return SectionPos.sectionToBlockCoord(nearestChunkX) + (blockX & SectionPos.SECTION_MASK);
    }

    static int nearestCopyBlockZ(WorldLoopTransformer transformer, int anchorChunkZ, int blockZ) {
        int nearestChunkZ = transformer.chunks.z.unwrap(anchorChunkZ, SectionPos.blockToSectionCoord(blockZ));
        return SectionPos.sectionToBlockCoord(nearestChunkZ) + (blockZ & SectionPos.SECTION_MASK);
    }

    // The block within the chunk is the low bits of the position, which are the same in either copy.
    private static BlockPos blockInChunkCopy(ChunkPos clientChunk, BlockPos pos) {
        return new BlockPos(
                clientChunk.getMinBlockX() + (pos.getX() & SectionPos.SECTION_MASK),
                pos.getY(),
                clientChunk.getMinBlockZ() + (pos.getZ() & SectionPos.SECTION_MASK));
    }

    // A packet whose position sits in a private field can still be moved: the position is the first thing on the wire,
    // so the packet is re-encoded with a new one in front of its untouched remainder. Both position codecs are
    // fixed-size packed longs, so the client position re-encodes to the width the server one vacated and the target
    // holds exactly the source packet — no buffer growth.
    private static <T, P> T rewritePosition(StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, P> positionCodec, T packet, TranslationContext context,
            UnaryOperator<P> toClient) {
        RegistryFriendlyByteBuf source = buffer(context);
        codec.encode(source, packet);
        P serverPosition = positionCodec.decode(source);

        RegistryFriendlyByteBuf target = buffer(context, source.readerIndex() + source.readableBytes());
        positionCodec.encode(target, toClient.apply(serverPosition));
        target.writeBytes(source);
        return codec.decode(target);
    }

    // Unpooled.buffer()'s own default, spelled out because the factory always takes an explicit capacity.
    private static final int DEFAULT_BUFFER_CAPACITY = 256;

    private static RegistryFriendlyByteBuf buffer(TranslationContext context) {
        return context.bufferFactory().apply(DEFAULT_BUFFER_CAPACITY);
    }

    private static RegistryFriendlyByteBuf buffer(TranslationContext context, int capacity) {
        return context.bufferFactory().apply(capacity);
    }

    @SuppressWarnings("unchecked")
    private static <P extends Packet<?>> BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter(BiFunction<P, TranslationContext, Packet<?>> typed) {
        return (packet, player) -> typed.apply((P) packet, player);
    }

    private PacketTranslator() {
    }
}
