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
import com.toroidalworld.mixin.BlockPositionSourceAccessor;
import com.toroidalworld.mixin.LevelChunkPacketAccessor;
import com.toroidalworld.mixin.LightUpdatePacketAccessor;
import com.toroidalworld.mixin.PlayerLookAtPacketAccessor;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.player.ClientPosition.BorderCenter;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
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
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.RelativeMovement;
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

    // A section has no stream codec of its own on this version, but it has always travelled as the packed long its
    // own accessors read back, so the codec the swap below needs is two lines rather than a reason to rebuild the packet.
    private static final StreamCodec<FriendlyByteBuf, SectionPos> SECTION_POS_CODEC = StreamCodec.of(
            (buffer, section) -> buffer.writeLong(section.asLong()),
            buffer -> SectionPos.of(buffer.readLong()));

    // Three plain doubles — how a position sits on the wire in the packets that offer no way to rebuild them from
    // values: the entity teleport and the vehicle correction.
    private static final StreamCodec<FriendlyByteBuf, Vec3> POSITION_CODEC = StreamCodec.of(
            (buffer, position) -> {
                buffer.writeDouble(position.x);
                buffer.writeDouble(position.y);
                buffer.writeDouble(position.z);
            },
            buffer -> new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));

    // The hit point on an entity travels as three floats, not the doubles a position usually takes.
    private static final StreamCodec<FriendlyByteBuf, Vec3> HIT_LOCATION_CODEC = StreamCodec.of(
            (buffer, location) -> {
                buffer.writeFloat((float) location.x);
                buffer.writeFloat((float) location.y);
                buffer.writeFloat((float) location.z);
            },
            buffer -> new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));

    // What opens a serverbound interact, in front of the hit point: the entity, then which of the three actions
    // follows. The entity is the reason this is decoded at all — the packet exposes no getter for it, and the hit
    // point has to fold around the entity it names.
    private record InteractHeader(int entityId, int actionType) {
    }

    private static final StreamCodec<FriendlyByteBuf, InteractHeader> INTERACT_HEADER_CODEC = StreamCodec.of(
            (buffer, header) -> {
                buffer.writeVarInt(header.entityId());
                buffer.writeVarInt(header.actionType());
            },
            buffer -> new InteractHeader(buffer.readVarInt(), buffer.readVarInt()));

    // Nothing in front of the position: decodes without touching the buffer, so the prefix comes out zero bytes wide.
    private static final StreamCodec<FriendlyByteBuf, Unit> NO_PREFIX_CODEC = StreamCodec.unit(Unit.INSTANCE);

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
            Map.entry(ClientboundMoveVehiclePacket.class, rewriter(PacketTranslator::moveVehicle)),
            Map.entry(ClientboundBlockEventPacket.class, rewriter(PacketTranslator::blockEvent)),
            Map.entry(ClientboundOpenSignEditorPacket.class, rewriter(PacketTranslator::openSignEditor)),
            Map.entry(ClientboundLevelEventPacket.class, rewriter(PacketTranslator::levelEvent)),
            Map.entry(ClientboundSoundPacket.class, rewriter(PacketTranslator::sound)),
            Map.entry(ClientboundLevelParticlesPacket.class, rewriter(PacketTranslator::levelParticles)),
            Map.entry(ClientboundExplodePacket.class, rewriter(PacketTranslator::explode)),
            Map.entry(ClientboundSetDefaultSpawnPositionPacket.class, rewriter(PacketTranslator::setDefaultSpawnPosition)),
            Map.entry(ClientboundInitializeBorderPacket.class, rewriter(PacketTranslator::initializeBorder)),
            Map.entry(ClientboundSetBorderCenterPacket.class, rewriter(PacketTranslator::setBorderCenter)),
            Map.entry(ClientboundPlayerLookAtPacket.class, rewriter(PacketTranslator::playerLookAt)),
            Map.entry(ClientboundDamageEventPacket.class, rewriter(PacketTranslator::damageEvent)),
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
            Map.entry(ServerboundSignUpdatePacket.class, rewriter(PacketTranslator::signUpdate)),
            Map.entry(ServerboundBlockEntityTagQueryPacket.class, rewriter(PacketTranslator::blockEntityTagQuery)),
            Map.entry(ServerboundInteractPacket.class, rewriter(PacketTranslator::interact)),
            Map.entry(ServerboundJigsawGeneratePacket.class, rewriter(PacketTranslator::jigsawGenerate)),
            Map.entry(ServerboundSetCommandBlockPacket.class, rewriter(PacketTranslator::setCommandBlock)),
            Map.entry(ServerboundSetJigsawBlockPacket.class, rewriter(PacketTranslator::setJigsawBlock)),
            Map.entry(ServerboundSetStructureBlockPacket.class, rewriter(PacketTranslator::setStructureBlock)));

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
        accessor.toroidal$setX(clientPos.x);
        accessor.toroidal$setZ(clientPos.z);
        return packet;
    }

    private static ClientboundLightUpdatePacket lightUpdate(ClientboundLightUpdatePacket packet, TranslationContext context) {
        ChunkPos clientPos = context.toClient(new ChunkPos(packet.getX(), packet.getZ()));

        LightUpdatePacketAccessor accessor = (LightUpdatePacketAccessor) packet;
        accessor.toroidal$setX(clientPos.x);
        accessor.toroidal$setZ(clientPos.z);
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

    private static ClientboundSetChunkCacheCenterPacket chunkCacheCenter(ClientboundSetChunkCacheCenterPacket packet, TranslationContext context) {
        ChunkPos clientPos = context.toClient(new ChunkPos(packet.getX(), packet.getZ()));
        return new ClientboundSetChunkCacheCenterPacket(clientPos.x, clientPos.z);
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

        // A relative delta reaches the client as-is and moves its unbounded coordinate by that much. Folded to its
        // shortest equivalent through the seam it names the same physical arrival, but a lap-sized hop (a relative
        // move of about a world width) stops carrying the client a world over — where every chunk it holds would
        // re-anchor to a different copy and have to be re-sent.
        //
        // That fold is load-bearing here rather than a nicety. The teleport funnel takes absolute arguments on this
        // version and subtracts the player's position itself, so a destination the wrap hook pulled back inside the
        // bounds reaches the wire as a delta a whole world wide. Folding is modulo that width, so it comes back out as
        // the step the client would have been sent had nothing been wrapped.
        Set<RelativeMovement> relatives = packet.getRelativeArguments();
        boolean relativeX = relatives.contains(RelativeMovement.X);
        boolean relativeZ = relatives.contains(RelativeMovement.Z);
        double foldedX = relativeX ? context.transformer().coords.x.foldDelta(packet.getX()) : 0.0;
        double foldedZ = relativeZ ? context.transformer().coords.z.foldDelta(packet.getZ()) : 0.0;
        double clientX = relativeX ? clientPosition.x() + foldedX : context.nearestCopyX(packet.getX());
        double clientZ = relativeZ ? clientPosition.z() + foldedZ : context.nearestCopyZ(packet.getZ());
        PacketProbe.playerPosition(context.dimension(), relativeX, relativeZ,
                packet.getX(), relativeX ? foldedX : clientX,
                packet.getZ(), relativeZ ? foldedZ : clientZ,
                clientPosition.x(), clientPosition.z());
        clientPosition.set(clientX, clientZ);

        return new ClientboundPlayerPositionPacket(
                relativeX ? foldedX : clientX,
                packet.getY(),
                relativeZ ? foldedZ : clientZ,
                packet.getYRot(), packet.getXRot(), relatives, packet.getId());
    }

    // Entities are placed by absolute position when they appear or are teleported; their ordinary movement travels as a
    // delta from the last known position and needs no translation.
    private static ClientboundAddEntityPacket addEntity(ClientboundAddEntityPacket packet, TranslationContext context) {
        PacketReach reach = context.trackedReach();
        double clientX = context.toClientX(packet.getX(), reach);
        double clientZ = context.toClientZ(packet.getZ(), reach);
        PacketProbe.addEntity(context.dimension(), packet.getId(), packet.getX(), clientX, packet.getZ(), clientZ);
        return new ClientboundAddEntityPacket(
                packet.getId(), packet.getUUID(),
                clientX,
                packet.getY(),
                clientZ,
                packet.getXRot(), packet.getYRot(), packet.getType(), packet.getData(),
                new Vec3(packet.getXa(), packet.getYa(), packet.getZa()), packet.getYHeadRot());
    }

    // The rider predicts their own vehicle; a correction only arrives when the server disagrees, which the continuous
    // read prevents. Across the seam the server would still send one — its position jumped a world — and the client,
    // told to snap its own boat, resets interpolation and jolts. Dropping it leaves the smooth local prediction alone.
    // Only a vehicle the player actually steers is dropped: a minecart the server drives must keep being sent, or it
    // would freeze on the passenger's screen.
    //
    // An entity teleport carries no relative flags on this version — it is always an absolute position — so there is no
    // axis to leave alone. The packet also offers no constructor to rebuild from values, so the position is swapped on
    // the wire, behind the entity id that opens it.
    private static Packet<?> teleportEntity(ClientboundTeleportEntityPacket packet, TranslationContext context) {
        if (context.ownVehicle().test(packet.getId())) {
            PacketProbe.teleportEntityDropped(context.dimension());
            return null;
        }

        PacketReach reach = context.trackedReach();
        return rewritePosition(ClientboundTeleportEntityPacket.STREAM_CODEC, ByteBufCodecs.VAR_INT, POSITION_CODEC,
                packet, context, (entityId, position) -> {
                    Vec3 clientPosition = context.toClient(position, reach);
                    PacketProbe.teleportEntity(context.dimension(), entityId, position, clientPosition);
                    return clientPosition;
                });
    }

    // The correction only ever names the vehicle the recipient is riding, so it arrives from no distance at all; the
    // tracking reach is a generous bound rather than a tight one, and the tight one would be zero. It cannot be rebuilt
    // from values either, but here the position opens the packet, so nothing precedes it.
    private static Packet<?> moveVehicle(ClientboundMoveVehiclePacket packet, TranslationContext context) {
        PacketReach reach = context.trackedReach();
        return rewritePosition(ClientboundMoveVehiclePacket.STREAM_CODEC, POSITION_CODEC, packet, context,
                position -> {
                    Vec3 clientPosition = context.toClient(position, reach);
                    PacketProbe.moveVehicle(context.dimension(), position, clientPosition);
                    return clientPosition;
                });
    }

    private static ClientboundBlockUpdatePacket blockUpdate(ClientboundBlockUpdatePacket packet, TranslationContext context) {
        return new ClientboundBlockUpdatePacket(toClientBlock(context, packet.getPos()), packet.getBlockState());
    }

    // Several blocks changing in one section travel as a batch, and the section they belong to sits in a private field.
    private static ClientboundSectionBlocksUpdatePacket sectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, TranslationContext context) {
        return rewritePosition(ClientboundSectionBlocksUpdatePacket.STREAM_CODEC, SECTION_POS_CODEC, packet, context,
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

    // An ordinary level event happens in a chunk the listener holds, so it takes the chunk-anchored fold. A global one
    // — a wither waking, a dragon dying, the end portal opening — goes to everyone in the world at its true position,
    // and the client keeps only the direction to it (it plays the sound two blocks from its own camera along that
    // line). So it names a place the client does not hold, like a look-at target, and takes the plain nearest-copy
    // fold, outside the view-reach backstop that would otherwise call a legitimate packet a break.
    private static ClientboundLevelEventPacket levelEvent(ClientboundLevelEventPacket packet, TranslationContext context) {
        BlockPos clientPos = packet.isGlobalEvent()
                ? nearestCopyBlock(context, packet.getPos())
                : toClientBlock(context, packet.getPos());
        return new ClientboundLevelEventPacket(
                packet.getType(), clientPos, packet.getData(), packet.isGlobalEvent());
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
                packet.isOverrideLimiter(),
                clientOrigin.x, clientOrigin.y, clientOrigin.z,
                packet.getXDist(), packet.getYDist(), packet.getZDist(), packet.getMaxSpeed(), packet.getCount());
    }

    // Both particle payloads are spawned around the centre client-side, so the centre is the anchor for anything they
    // carry inside. The knockback is a velocity the client adds to itself and names no place, so it rides across as it
    // came — rebuilt from the three floats it was stored as, which is exact in both directions.
    private static ClientboundExplodePacket explode(ClientboundExplodePacket packet, TranslationContext context) {
        Vec3 serverCenter = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        Vec3 clientCenter = context.toClient(serverCenter, PacketReach.EXPLOSION);
        PacketProbe.explode(context.dimension(), serverCenter, clientCenter, packet.getToBlow().size(),
                Mth.floor(clientCenter.x) - Mth.floor(serverCenter.x),
                Mth.floor(clientCenter.z) - Mth.floor(serverCenter.z));
        return new ClientboundExplodePacket(
                clientCenter.x, clientCenter.y, clientCenter.z, packet.getPower(),
                toClientBlown(packet.getToBlow(), serverCenter, clientCenter),
                new Vec3(packet.getKnockbackX(), packet.getKnockbackY(), packet.getKnockbackZ()),
                packet.getBlockInteraction(),
                toClientParticle(context, packet.getSmallExplosionParticles(), clientCenter),
                toClientParticle(context, packet.getLargeExplosionParticles(), clientCenter),
                packet.getExplosionSound());
    }

    // The blocks the blast destroyed are absolute positions, but they travel as signed byte deltas from the packet's
    // own centre and the client rebuilds them by adding those deltas back to the centre it was handed. Moving the
    // centre and leaving them would make every delta a world wide, which does not fit in a byte: the client would tear
    // out blocks scattered anywhere but under the explosion. They move by exactly the offset the centre moved, the same
    // offset for all of them, so every delta keeps the width it already had — a blast reaches a few blocks, so a centre
    // that folded folded its blocks the same way. The shift is taken between the floored centres because those are the
    // very numbers the packet writes its deltas against.
    private static List<BlockPos> toClientBlown(List<BlockPos> blown, Vec3 serverCenter, Vec3 clientCenter) {
        int shiftX = Mth.floor(clientCenter.x) - Mth.floor(serverCenter.x);
        int shiftZ = Mth.floor(clientCenter.z) - Mth.floor(serverCenter.z);
        if (blown.isEmpty() || (shiftX == 0 && shiftZ == 0)) {
            return blown;
        }

        List<BlockPos> translated = new ArrayList<>(blown.size());
        for (BlockPos pos : blown) {
            translated.add(pos.offset(shiftX, 0, shiftZ));
        }

        return translated;
    }

    // A particle payload can carry a second, absolute position of its own, and the packet coordinate it rides on has
    // just been moved a whole world; left as it came, the particle is drawn from a translated start toward a raw
    // server coordinate. It is folded to the copy nearest that start — a target the client can actually fly to.
    //
    // Which fold differs by what the position names. A vibration's sensor is a loose point a few blocks from the start
    // and takes the plain nearest copy. A block particle's position names a block whose model data the client looks up,
    // so it takes the chunk-anchored fold: it has to land in the copy of the chunk the client holds, or the lookup
    // resolves against nothing and falls back to the default sprite.
    //
    // A vibration travelling to a warden or an allay carries an entity position source, which is an entity id on the
    // wire and resolves to the client's own copy — already in client space, and nothing to move.
    private static ParticleOptions toClientParticle(TranslationContext context, ParticleOptions particle,
            Vec3 clientOrigin) {
        switch (particle) {
            case VibrationParticleOption vibration -> {
                if (!(vibration.getDestination() instanceof BlockPositionSource destination)) {
                    return particle;
                }

                BlockPos serverDestination = ((BlockPositionSourceAccessor) destination).toroidal$getPos();
                BlockPos clientDestination = context.transformer().blocks.nearestCopy(
                        BlockPos.containing(clientOrigin), serverDestination);
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

    // The compass needle is computed in the client's unbounded space, so the spawn is moved to the copy nearest the
    // player — a directional hint that may legitimately sit far beyond the view, like a global event. The packet carries a
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
    // hint like a global event, not a coordinate the player's nearness put on the wire. It takes the plain nearest-copy
    // fold for the same reason a global event does.
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
                : new BlockHitResult(location, hit.getDirection(), pos, hit.isInside());
        return new ServerboundUseItemOnPacket(packet.getHand(), wrapped, packet.getSequence());
    }

    private static ServerboundPlayerActionPacket playerAction(ServerboundPlayerActionPacket packet, TranslationContext context) {
        return new ServerboundPlayerActionPacket(packet.getAction(), context.toServer(packet.getPos()),
                packet.getDirection(), packet.getSequence());
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
    //
    // The packet keeps all three of the entity, the hand and the point inside a private action object, and the only
    // ways in are three factories that want a live Entity. So it is rewritten on the wire instead: the two varints
    // that open it are copied across and the point behind them re-encoded, which leaves the hand and the action
    // untouched by construction.
    private static ServerboundInteractPacket interact(ServerboundInteractPacket packet, TranslationContext context) {
        if (!carriesLocation(packet)) {
            PacketProbe.interactNoLocation(context.dimension());
            return packet;
        }

        return rewritePosition(ServerboundInteractPacket.STREAM_CODEC, INTERACT_HEADER_CODEC, HIT_LOCATION_CODEC,
                packet, context, (header, location) -> toServerHitLocation(context, header.entityId(), location));
    }

    private static Vec3 toServerHitLocation(TranslationContext context, int entityId, Vec3 location) {
        Vec3 targetPosition = context.entityPosition().apply(entityId);
        Vec3 serverLocation = targetPosition == null
                ? context.toServer(location)
                : context.transformer().vectors.nearestCopy(targetPosition, location);
        PacketProbe.interact(context.dimension(), entityId, location, serverLocation);
        return serverLocation;
    }

    // Whether this interact carries a point at all — only the at-location form does, and an attack or a plain
    // interaction has a boolean where the point would be, so reading one would be reading the wrong bytes. The packet
    // is asked through the same dispatch the server itself uses, because the action enum behind it is not public.
    private static boolean carriesLocation(ServerboundInteractPacket packet) {
        boolean[] atLocation = new boolean[1];
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(InteractionHand hand) {
            }

            @Override
            public void onInteraction(InteractionHand hand, Vec3 location) {
                atLocation[0] = true;
            }

            @Override
            public void onAttack() {
            }
        });

        return atLocation[0];
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
                packet.getData(), packet.isIgnoreEntities(), packet.isShowAir(),
                packet.isShowBoundingBox(), packet.getIntegrity(), packet.getSeed());
    }

    // A block update has to land on the copy of the chunk the client is actually holding, which is the one it was sent
    // under — not the one this position would map to now that the player has moved.
    static BlockPos toClientBlock(TranslationContext context, BlockPos pos) {
        return blockInChunkCopy(context.toClient(new ChunkPos(pos)), pos);
    }

    // A global event, a world spawn, a look-at target: a directional hint, not a block in a held chunk — so it is
    // translated outside the view-reach backstop, which only bounds traffic the client's own nearness put on the wire.
    private static BlockPos nearestCopyBlock(TranslationContext context, BlockPos pos) {
        return nearestCopyBlock(context.transformer(), context.clientPosition().chunk(), pos);
    }

    // Also the ClientAnchorSync flip check — recomputed outside a packet flow, so the math is shared here and the
    // stored copy can never disagree with what a re-send would produce. The per-axis forms are the primitive truth:
    // the tick-side check reads them directly and builds nothing on its steady-state path.
    static BlockPos nearestCopyBlock(WorldLoopTransformer transformer, ChunkPos anchor, BlockPos pos) {
        return new BlockPos(
                nearestCopyBlockX(transformer, anchor.x, pos.getX()),
                pos.getY(),
                nearestCopyBlockZ(transformer, anchor.z, pos.getZ()));
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
    // so the packet is re-encoded with a new one in front of its untouched remainder.
    private static <T, P> T rewritePosition(StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, P> positionCodec, T packet, TranslationContext context,
            UnaryOperator<P> toClient) {
        return rewritePosition(codec, NO_PREFIX_CODEC, positionCodec, packet, context,
                (noPrefix, serverPosition) -> toClient.apply(serverPosition));
    }

    // The same swap where the position is not first on the wire — an entity id stands in front of it, or an id and an
    // action. The prefix is decoded so its width is known and then copied across byte for byte: re-encoding it would
    // have to reproduce it exactly, and a copy owes nothing to that. Its decoded value is handed to the mapper because
    // for a serverbound interact those leading bytes are the only place the entity id lives — the packet exposes no
    // getter for it, and the hit location has to fold around the entity it names.
    //
    // Every position codec that comes through here re-encodes to the width the server one vacated — a packed long,
    // three doubles, three floats — so the target holds exactly the source packet and the buffer never grows.
    private static <T, R, P> T rewritePosition(StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, R> prefixCodec,
            StreamCodec<? super RegistryFriendlyByteBuf, P> positionCodec, T packet, TranslationContext context,
            BiFunction<R, P, P> toClient) {
        RegistryFriendlyByteBuf source = buffer(context);
        codec.encode(source, packet);
        R prefix = prefixCodec.decode(source);
        int prefixLength = source.readerIndex();
        P serverPosition = positionCodec.decode(source);

        RegistryFriendlyByteBuf target = buffer(context, source.readerIndex() + source.readableBytes());
        target.writeBytes(source, 0, prefixLength);
        positionCodec.encode(target, toClient.apply(prefix, serverPosition));
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
