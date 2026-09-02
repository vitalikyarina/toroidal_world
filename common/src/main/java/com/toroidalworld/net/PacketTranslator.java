package com.toroidalworld.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.SeamDelta;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.mixin.BlockEntityDataPacketAccessor;
import com.toroidalworld.mixin.BlockPositionSourceAccessor;
import com.toroidalworld.mixin.InitializeBorderPacketAccessor;
import com.toroidalworld.mixin.InteractPacketAccessor;
import com.toroidalworld.mixin.LevelChunkPacketAccessor;
import com.toroidalworld.mixin.LightUpdatePacketAccessor;
import com.toroidalworld.mixin.MoveVehiclePacketAccessor;
import com.toroidalworld.mixin.PlayerLookAtPacketAccessor;
import com.toroidalworld.mixin.SectionBlocksUpdatePacketAccessor;
import com.toroidalworld.mixin.SetBorderCenterPacketAccessor;
import com.toroidalworld.mixin.TeleportEntityPacketAccessor;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.player.ClientPosition.BorderCenter;
import com.toroidalworld.player.MirrorWriter;
import com.toroidalworld.registry.StartupRegistry;
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
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
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
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PacketTranslator {
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

    private static final StreamCodec<FriendlyByteBuf, Vec3> POSITION_CODEC = StreamCodec.of(
            (buffer, position) -> {
                buffer.writeDouble(position.x);
                buffer.writeDouble(position.y);
                buffer.writeDouble(position.z);
            },
            buffer -> new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));

    private static final StreamCodec<FriendlyByteBuf, Vec3> HIT_LOCATION_CODEC = StreamCodec.of(
            (buffer, location) -> {
                buffer.writeFloat((float) location.x);
                buffer.writeFloat((float) location.y);
                buffer.writeFloat((float) location.z);
            },
            buffer -> new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));

    private record InteractHeader(int entityId, int actionType) {
    }

    private static final StreamCodec<FriendlyByteBuf, InteractHeader> INTERACT_HEADER_CODEC = StreamCodec.of(
            (buffer, header) -> {
                buffer.writeVarInt(header.entityId());
                buffer.writeVarInt(header.actionType());
            },
            buffer -> new InteractHeader(buffer.readVarInt(), buffer.readVarInt()));

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
            Map.entry(ClientboundCustomPayloadPacket.class, rewriter(PacketTranslator::clientboundCustomPayload)));

    private static final PacketRewriters PRODUCTION = new PacketRewriters();

    static PacketRewriters production() {
        return PRODUCTION;
    }

    public static <P extends CustomPacketPayload> void registerClientboundPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        PRODUCTION.registerClientboundPayload(payloadType, payloadRewriter);
    }

    public static <P extends CustomPacketPayload> void registerServerboundPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        PRODUCTION.registerServerboundPayload(payloadType, payloadRewriter);
    }

    public static <P extends ParticleOptions> void registerParticleRewriter(Class<P> particleType,
            PacketRewriters.ParticleRewriter<P> particleRewriter) {
        PRODUCTION.registerParticle(particleType, particleRewriter);
    }

    public static <T> void registerEntityDataRewriter(EntityDataSerializer<T> serializer,
            PacketRewriters.EntityDataRewriter<T> dataRewriter) {
        PRODUCTION.registerEntityData(serializer, dataRewriter);
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
            Map.entry(ServerboundSetStructureBlockPacket.class, rewriter(PacketTranslator::setStructureBlock)),
            Map.entry(ServerboundCustomPayloadPacket.class, rewriter(PacketTranslator::serverboundCustomPayload)));

    public static <T extends net.minecraft.network.PacketListener> Packet<T> toClient(Packet<T> packet, ServerPlayer player) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
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

    @SuppressWarnings("unchecked")
    private static Packet<?> toClientBundle(ClientboundBundlePacket bundle, ServerPlayer player, WorldFold transformer) {
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
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
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
            Packet<T> packet, ServerPlayer player, WorldFold transformer) {
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

    private static ClientboundLevelChunkWithLightPacket levelChunk(ClientboundLevelChunkWithLightPacket packet, TranslationContext context) {
        ChunkPos serverPos = new ChunkPos(packet.getX(), packet.getZ());
        ChunkPos clientPos = context.toClient(serverPos, ChunkTraffic.CHUNK_DATA);

        LevelChunkPacketAccessor accessor = (LevelChunkPacketAccessor) packet;
        accessor.toroidal$setX(clientPos.x);
        accessor.toroidal$setZ(clientPos.z);
        return packet;
    }

    private static ClientboundLightUpdatePacket lightUpdate(ClientboundLightUpdatePacket packet, TranslationContext context) {
        ChunkPos clientPos = context.toClient(new ChunkPos(packet.getX(), packet.getZ()), ChunkTraffic.LIGHT_UPDATE);

        LightUpdatePacketAccessor accessor = (LightUpdatePacketAccessor) packet;
        accessor.toroidal$setX(clientPos.x);
        accessor.toroidal$setZ(clientPos.z);
        return packet;
    }

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
        ChunkPos clientPos = context.toClientCacheCenter(new ChunkPos(packet.getX(), packet.getZ()));
        return new ClientboundSetChunkCacheCenterPacket(clientPos.x, clientPos.z);
    }

    private static Packet<?> clientboundCustomPayload(ClientboundCustomPayloadPacket packet, TranslationContext context) {
        CustomPacketPayload rewritten = context.rewriters().rewriteClientbound(packet.payload(), context);
        return rewritten == packet.payload() ? packet : new ClientboundCustomPayloadPacket(rewritten);
    }

    private static Packet<?> serverboundCustomPayload(ServerboundCustomPayloadPacket packet, TranslationContext context) {
        CustomPacketPayload rewritten = context.rewriters().rewriteServerbound(packet.payload(), context);
        return rewritten == packet.payload() ? packet : new ServerboundCustomPayloadPacket(rewritten);
    }

    private static ClientboundPlayerPositionPacket playerPosition(ClientboundPlayerPositionPacket packet, TranslationContext context) {
        ClientPosition clientPosition = context.clientPosition();

        if (!clientPosition.describes(context.dimension())) {
            context.rebase().run();
            return packet;
        }

        Set<RelativeMovement> relatives = packet.getRelativeArguments();
        boolean relativeX = relatives.contains(RelativeMovement.X);
        boolean relativeZ = relatives.contains(RelativeMovement.Z);
        double foldedX = relativeX ? SeamDelta.foldX(context.transformer(), packet.getX()) : 0.0;
        double foldedZ = relativeZ ? SeamDelta.foldZ(context.transformer(), packet.getZ()) : 0.0;
        double clientX = relativeX ? clientPosition.x() + foldedX : context.nearestCopyX(packet.getX());
        double clientZ = relativeZ ? clientPosition.z() + foldedZ : context.nearestCopyZ(packet.getZ());
        clientPosition.set(clientX, clientZ, MirrorWriter.POSITION_PACKET);

        return new ClientboundPlayerPositionPacket(
                relativeX ? foldedX : clientX,
                packet.getY(),
                relativeZ ? foldedZ : clientZ,
                packet.getYRot(), packet.getXRot(), relatives, packet.getId());
    }

    private static ClientboundAddEntityPacket addEntity(ClientboundAddEntityPacket packet, TranslationContext context) {
        PacketReach reach = context.trackedReach();
        double clientX = context.toClientX(packet.getX(), reach);
        double clientZ = context.toClientZ(packet.getZ(), reach);
        return new ClientboundAddEntityPacket(
                packet.getId(), packet.getUUID(),
                clientX,
                packet.getY(),
                clientZ,
                packet.getXRot(), packet.getYRot(), packet.getType(), packet.getData(),
                new Vec3(packet.getXa(), packet.getYa(), packet.getZa()), packet.getYHeadRot());
    }

    // An entity teleport carries no relative flags on this version — it is always an absolute position — so there is no
    // axis to leave alone. The packet also offers no constructor to rebuild from values, so the position is swapped on
    // the wire, behind the entity id that opens it.
    private static Packet<?> teleportEntity(ClientboundTeleportEntityPacket packet, TranslationContext context) {
        if (context.ownVehicle().test(packet.getId())) {
            return null;
        }

        PacketReach reach = context.trackedReach();
        return rewritePosition(
                (teleportPacket, output) -> ((TeleportEntityPacketAccessor) teleportPacket).toroidal$write(output),
                TeleportEntityPacketAccessor::toroidal$create,
                ByteBufCodecs.VAR_INT, POSITION_CODEC,
                packet, context, (entityId, position) -> context.toClient(position, reach));
    }

    private static Packet<?> moveVehicle(ClientboundMoveVehiclePacket packet, TranslationContext context) {
        PacketReach reach = context.trackedReach();
        return rewritePosition(
                (vehiclePacket, output) -> ((MoveVehiclePacketAccessor) vehiclePacket).toroidal$write(output),
                MoveVehiclePacketAccessor::toroidal$create,
                POSITION_CODEC, packet, context,
                position -> context.toClient(position, reach));
    }

    private static ClientboundBlockUpdatePacket blockUpdate(ClientboundBlockUpdatePacket packet, TranslationContext context) {
        return new ClientboundBlockUpdatePacket(
                toClientBlock(context, packet.getPos(), ChunkTraffic.BLOCK_UPDATE), packet.getBlockState());
    }

    private static ClientboundSectionBlocksUpdatePacket sectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, TranslationContext context) {
        return rewritePosition(
                (sectionPacket, output) -> ((SectionBlocksUpdatePacketAccessor) sectionPacket).toroidal$write(output),
                SectionBlocksUpdatePacketAccessor::toroidal$create,
                SECTION_POS_CODEC, packet, context,
                section -> SectionPos.of(context.toClient(section.chunk(), ChunkTraffic.SECTION_BLOCKS), section.y()));
    }

    private static ClientboundBlockEntityDataPacket blockEntityData(ClientboundBlockEntityDataPacket packet, TranslationContext context) {
        return BlockEntityDataPacketAccessor.toroidal$create(
                toClientBlock(context, packet.getPos(), ChunkTraffic.BLOCK_ENTITY), packet.getType(), packet.getTag());
    }

    private static ClientboundBlockDestructionPacket blockDestruction(ClientboundBlockDestructionPacket packet, TranslationContext context) {
        return new ClientboundBlockDestructionPacket(
                packet.getId(), toClientBlock(context, packet.getPos(), ChunkTraffic.BLOCK_DESTRUCTION),
                packet.getProgress());
    }

    private static ClientboundLevelEventPacket levelEvent(ClientboundLevelEventPacket packet, TranslationContext context) {
        BlockPos clientPos = packet.isGlobalEvent()
                ? nearestCopyBlock(context, packet.getPos())
                : toClientBlock(context, packet.getPos(), ChunkTraffic.LEVEL_EVENT);
        return new ClientboundLevelEventPacket(
                packet.getType(), clientPos, packet.getData(), packet.isGlobalEvent());
    }

    private static ClientboundSetEntityDataPacket setEntityData(ClientboundSetEntityDataPacket packet, TranslationContext context) {
        List<SynchedEntityData.DataValue<?>> items = packet.packedItems();
        Supplier<Vec3> anchor = entityAnchor(packet.id(), context);
        List<SynchedEntityData.DataValue<?>> translated = new ArrayList<>(items.size());
        boolean changed = false;
        for (SynchedEntityData.DataValue<?> item : items) {
            SynchedEntityData.DataValue<?> clientItem = toClientData(item, anchor, context);
            changed |= clientItem != item;
            translated.add(clientItem);
        }

        return changed ? new ClientboundSetEntityDataPacket(packet.id(), translated) : packet;
    }

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

    private static Supplier<Vec3> entityAnchor(int entityId, TranslationContext context) {
        return new Supplier<>() {
            private @Nullable Vec3 anchor;

            @Override
            public Vec3 get() {
                if (anchor == null) {
                    anchor = resolveEntityAnchor(entityId, context);
                }

                return anchor;
            }
        };
    }

    private static Vec3 resolveEntityAnchor(int entityId, TranslationContext context) {
        Vec3 serverPosition = context.entityPosition().apply(entityId);
        if (serverPosition == null) {
            ClientPosition mirror = context.clientPosition();
            return new Vec3(mirror.x(), 0.0, mirror.z());
        }

        return context.toClient(serverPosition, context.trackedReach());
    }

    private static SynchedEntityData.DataValue<?> toClientData(SynchedEntityData.DataValue<?> item,
            Supplier<Vec3> anchor, TranslationContext context) {
        PacketRewriters.EntityDataRewriter<Object> dataRewriter = context.rewriters().entityDataFor(item);
        if (dataRewriter != null) {
            Object rewritten = dataRewriter.rewrite(item.value(), context, anchor.get());
            return rewritten == item.value() ? item : withValue(item, rewritten);
        }

        Object value = item.value();
        if (value instanceof Optional<?> optional) {
            Object held = optional.orElse(null);
            if (held == null) {
                return item;
            }

            Object clientHeld = toClientValue(held, anchor, context);
            return clientHeld == held ? item : withValue(item, Optional.of(clientHeld));
        }

        Object clientValue = toClientValue(value, anchor, context);
        return clientValue == value ? item : withValue(item, clientValue);
    }

    private static Object toClientValue(Object value, Supplier<Vec3> anchor, TranslationContext context) {
        return switch (value) {
            case BlockPos pos -> nearestCopyBlock(context, anchor.get(), pos);
            case GlobalPos globalPos -> toClientGlobal(globalPos, anchor, context);
            case ParticleOptions particle -> toClientParticle(context, particle, anchor.get());
            case List<?> values when isParticleList(values) -> toClientParticles(values, anchor.get(), context);
            default -> value;
        };
    }

    private static GlobalPos toClientGlobal(GlobalPos globalPos, Supplier<Vec3> anchor, TranslationContext context) {
        if (!globalPos.dimension().equals(context.dimension())) {
            return globalPos;
        }

        BlockPos clientPos = nearestCopyBlock(context, anchor.get(), globalPos.pos());
        return clientPos == globalPos.pos() ? globalPos : GlobalPos.of(globalPos.dimension(), clientPos);
    }

    private static List<?> toClientParticles(List<?> values, Vec3 anchor, TranslationContext context) {
        List<ParticleOptions> clientParticles = new ArrayList<>(values.size());
        boolean changed = false;
        for (Object value : values) {
            ParticleOptions particle = (ParticleOptions) value;
            ParticleOptions clientParticle = toClientParticle(context, particle, anchor);
            changed |= clientParticle != particle;
            clientParticles.add(clientParticle);
        }

        return changed ? clientParticles : values;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SynchedEntityData.DataValue<?> withValue(SynchedEntityData.DataValue<?> item, Object value) {
        return new SynchedEntityData.DataValue(item.id(), item.serializer(), value);
    }

    private static ClientboundBlockEventPacket blockEvent(ClientboundBlockEventPacket packet, TranslationContext context) {
        return new ClientboundBlockEventPacket(
                toClientBlock(context, packet.getPos(), ChunkTraffic.BLOCK_EVENT),
                packet.getBlock(), packet.getB0(), packet.getB1());
    }

    private static ClientboundOpenSignEditorPacket openSignEditor(ClientboundOpenSignEditorPacket packet, TranslationContext context) {
        return new ClientboundOpenSignEditorPacket(
                toClientBlock(context, packet.getPos(), ChunkTraffic.SIGN_EDITOR), packet.isFrontText());
    }

    private static ClientboundSoundPacket sound(ClientboundSoundPacket packet, TranslationContext context) {
        PacketReach reach = PacketReach.sound(packet.getSound().value().getRange(packet.getVolume()));
        Vec3 clientPos = context.toClient(new Vec3(packet.getX(), packet.getY(), packet.getZ()), reach);
        return new ClientboundSoundPacket(
                packet.getSound(), packet.getSource(),
                clientPos.x, clientPos.y, clientPos.z,
                packet.getVolume(), packet.getPitch(), packet.getSeed());
    }

    private static ClientboundLevelParticlesPacket levelParticles(ClientboundLevelParticlesPacket packet, TranslationContext context) {
        PacketReach reach = packet.isOverrideLimiter() ? PacketReach.FORCED_PARTICLE : PacketReach.PARTICLE;
        Vec3 clientOrigin = context.toClient(new Vec3(packet.getX(), packet.getY(), packet.getZ()), reach);
        return new ClientboundLevelParticlesPacket(
                toClientParticle(context, packet.getParticle(), clientOrigin),
                packet.isOverrideLimiter(),
                clientOrigin.x, clientOrigin.y, clientOrigin.z,
                packet.getXDist(), packet.getYDist(), packet.getZDist(), packet.getMaxSpeed(), packet.getCount());
    }

    private static ClientboundExplodePacket explode(ClientboundExplodePacket packet, TranslationContext context) {
        Vec3 serverCenter = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        Vec3 clientCenter = context.toClient(serverCenter, PacketReach.EXPLOSION);
        return new ClientboundExplodePacket(
                clientCenter.x, clientCenter.y, clientCenter.z, packet.getPower(),
                toClientBlown(packet.getToBlow(), serverCenter, clientCenter),
                new Vec3(packet.getKnockbackX(), packet.getKnockbackY(), packet.getKnockbackZ()),
                packet.getBlockInteraction(),
                toClientParticle(context, packet.getSmallExplosionParticles(), clientCenter),
                toClientParticle(context, packet.getLargeExplosionParticles(), clientCenter),
                packet.getExplosionSound());
    }

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

    private static ParticleOptions toClientParticle(TranslationContext context, ParticleOptions particle,
            Vec3 clientOrigin) {
        switch (particle) {
            case VibrationParticleOption vibration -> {
                if (!(vibration.getDestination() instanceof BlockPositionSource destination)) {
                    return particle;
                }

                BlockPos serverDestination = ((BlockPositionSourceAccessor) destination).toroidal$getPos();
                BlockPos clientDestination = nearestCopyBlock(context, clientOrigin, serverDestination);
                return new VibrationParticleOption(
                        new BlockPositionSource(clientDestination), vibration.getArrivalInTicks());
            }
            default -> {
                PacketRewriters.ParticleRewriter<ParticleOptions> particleRewriter = context.rewriters().particleFor(particle);
                return particleRewriter == null ? particle : particleRewriter.rewrite(particle, context, clientOrigin);
            }
        }
    }

    private static Packet<?> setDefaultSpawnPosition(ClientboundSetDefaultSpawnPositionPacket packet, TranslationContext context) {
        if (!Level.OVERWORLD.equals(context.dimension())) {
            return packet;
        }

        BlockPos serverPos = packet.getPos();
        BlockPos clientPos = nearestCopyBlock(context, serverPos);
        context.clientPosition().setHeldSpawn(clientPos);
        return new ClientboundSetDefaultSpawnPositionPacket(clientPos, packet.getAngle());
    }

    private static Packet<?> initializeBorder(ClientboundInitializeBorderPacket packet, TranslationContext context) {
        if (!context.clientPosition().describes(context.dimension())) {
            return packet;
        }

        return rewritePosition(
                (borderPacket, output) -> ((InitializeBorderPacketAccessor) borderPacket).toroidal$write(output),
                InitializeBorderPacketAccessor::toroidal$create,
                BORDER_CENTER_CODEC, packet, context,
                center -> toClientBorderCenter(context, center));
    }

    private static Packet<?> setBorderCenter(ClientboundSetBorderCenterPacket packet, TranslationContext context) {
        if (!context.clientPosition().describes(context.dimension())) {
            return packet;
        }

        return rewritePosition(
                (borderPacket, output) -> ((SetBorderCenterPacketAccessor) borderPacket).toroidal$write(output),
                SetBorderCenterPacketAccessor::toroidal$create,
                BORDER_CENTER_CODEC, packet, context,
                center -> toClientBorderCenter(context, center));
    }

    private static BorderCenter toClientBorderCenter(TranslationContext context, BorderCenter center) {
        ClientPosition clientPosition = context.clientPosition();
        BorderCenter clientCenter = nearestCopyCenter(context.transformer(), clientPosition, center);
        clientPosition.setHeldBorderCenter(clientCenter);
        return clientCenter;
    }

    static BorderCenter nearestCopyCenter(WorldFold transformer, ClientPosition clientPosition,
            BorderCenter center) {
        Vec3 nearest = transformer.nearestCopy(
                new Vec3(clientPosition.x(), 0.0, clientPosition.z()), new Vec3(center.x(), 0.0, center.z()));
        return new BorderCenter(nearest.x, nearest.z);
    }

    private static ClientboundPlayerLookAtPacket playerLookAt(ClientboundPlayerLookAtPacket packet, TranslationContext context) {
        PlayerLookAtPacketAccessor accessor = (PlayerLookAtPacketAccessor) packet;
        Vec3 near = context.nearestCopy(new Vec3(accessor.toroidal$getX(), 0.0, accessor.toroidal$getZ()));
        accessor.toroidal$setX(near.x);
        accessor.toroidal$setZ(near.z);
        return packet;
    }

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
                        context.toClient(data.pos(), ChunkTraffic.CHUNK_BIOMES), data.buffer()))
                .toList());
    }

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

    private static ServerboundInteractPacket interact(ServerboundInteractPacket packet, TranslationContext context) {
        if (!carriesLocation(packet)) {
            return packet;
        }

        return rewritePosition(
                (interactPacket, output) -> ((InteractPacketAccessor) interactPacket).toroidal$write(output),
                InteractPacketAccessor::toroidal$create,
                INTERACT_HEADER_CODEC, HIT_LOCATION_CODEC,
                packet, context, (header, location) -> toServerHitLocation(context, header.entityId(), location));
    }

    private static Vec3 toServerHitLocation(TranslationContext context, int entityId, Vec3 location) {
        Vec3 targetPosition = context.entityPosition().apply(entityId);
        return targetPosition == null
                ? context.toServer(location)
                : context.transformer().nearestCopy(targetPosition, location);
    }

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

    private static ServerboundSetStructureBlockPacket setStructureBlock(ServerboundSetStructureBlockPacket packet, TranslationContext context) {
        return new ServerboundSetStructureBlockPacket(
                context.toServer(packet.getPos()), packet.getUpdateType(), packet.getMode(),
                packet.getName(), packet.getOffset(), packet.getSize(), packet.getMirror(), packet.getRotation(),
                packet.getData(), packet.isIgnoreEntities(), packet.isShowAir(),
                packet.isShowBoundingBox(), packet.getIntegrity(), packet.getSeed());
    }

    static BlockPos toClientBlock(TranslationContext context, BlockPos pos, ChunkTraffic traffic) {
        return context.transformer().reseat(pos, context.toClient(new ChunkPos(pos), traffic));
    }

    private static BlockPos nearestCopyBlock(TranslationContext context, BlockPos pos) {
        return context.nearestCopy(pos);
    }

    private static BlockPos nearestCopyBlock(TranslationContext context, Vec3 anchor, BlockPos pos) {
        return context.transformer().nearestCopy(BlockPos.containing(anchor), pos);
    }

    static BlockPos nearestCopyBlock(WorldFold transformer, ChunkPos anchor, BlockPos pos) {
        return transformer.reseat(pos, transformer.nearestCopy(anchor, new ChunkPos(pos)));
    }

    private static <T, P> T rewritePosition(BiConsumer<T, RegistryFriendlyByteBuf> writer,
            Function<FriendlyByteBuf, T> reader,
            StreamCodec<? super RegistryFriendlyByteBuf, P> positionCodec, T packet, TranslationContext context,
            UnaryOperator<P> toClient) {
        return rewritePosition(writer, reader, NO_PREFIX_CODEC, positionCodec, packet, context,
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
    //
    // The wire pair is vanilla's own write/read behind the packet's STREAM_CODEC, reached through invokers rather
    // than the codec field: loaders and mods swap wrappers into those fields whose transforms assume the network
    // pipeline around them — Fabric's PacketContext is a scoped value bound only inside the encoder, and this runs
    // on the server thread — and the pipeline still encodes the finished packet, so a wrapper run here would also
    // run twice.
    private static <T, R, P> T rewritePosition(BiConsumer<T, RegistryFriendlyByteBuf> writer,
            Function<FriendlyByteBuf, T> reader,
            StreamCodec<? super RegistryFriendlyByteBuf, R> prefixCodec,
            StreamCodec<? super RegistryFriendlyByteBuf, P> positionCodec, T packet, TranslationContext context,
            BiFunction<R, P, P> toClient) {
        RegistryFriendlyByteBuf source = buffer(context);
        writer.accept(packet, source);
        R prefix = prefixCodec.decode(source);
        int prefixLength = source.readerIndex();
        P serverPosition = positionCodec.decode(source);

        RegistryFriendlyByteBuf target = buffer(context, source.readerIndex() + source.readableBytes());
        target.writeBytes(source, 0, prefixLength);
        positionCodec.encode(target, toClient.apply(prefix, serverPosition));
        target.writeBytes(source);
        return reader.apply(target);
    }

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
