package com.toroidalworld.net;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.toroidalworld.mixin.ChunkWaypointAccessor;
import com.toroidalworld.mixin.InitializeBorderPacketAccessor;
import com.toroidalworld.mixin.LevelChunkPacketAccessor;
import com.toroidalworld.mixin.LightUpdatePacketAccessor;
import com.toroidalworld.mixin.PlayerLookAtPacketAccessor;
import com.toroidalworld.mixin.SectionBlocksUpdatePacketAccessor;
import com.toroidalworld.mixin.SetBorderCenterPacketAccessor;
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

public final class PacketTranslator {
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

    private static final Map<Class<?>, BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>> PAYLOAD_REWRITERS =
            new HashMap<>();

    public static <P extends CustomPacketPayload> void registerPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        PAYLOAD_REWRITERS.put(payloadType,
                (payload, context) -> payloadRewriter.apply(payloadType.cast(payload), context));
    }

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
        ChunkPos clientPos = context.toClient(serverPos);

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
        return new ClientboundSetChunkCacheCenterPacket(clientPos.x(), clientPos.z());
    }

    private static Packet<?> customPayload(ClientboundCustomPayloadPacket packet, TranslationContext context) {
        BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> payloadRewriter =
                PAYLOAD_REWRITERS.get(packet.payload().getClass());
        if (payloadRewriter == null) {
            return packet;
        }

        CustomPacketPayload rewritten = payloadRewriter.apply(packet.payload(), context);
        return rewritten == packet.payload() ? packet : new ClientboundCustomPayloadPacket(rewritten);
    }

    private static ClientboundPlayerPositionPacket playerPosition(ClientboundPlayerPositionPacket packet, TranslationContext context) {
        ClientPosition clientPosition = context.clientPosition();

        if (!clientPosition.describes(context.dimension())) {
            context.rebase().run();
            return packet;
        }

        PositionMoveRotation change = packet.change();
        Vec3 position = change.position();

        boolean relativeX = packet.relatives().contains(Relative.X);
        boolean relativeZ = packet.relatives().contains(Relative.Z);
        double foldedX = relativeX ? SeamDelta.foldX(context.transformer(), position.x) : 0.0;
        double foldedZ = relativeZ ? SeamDelta.foldZ(context.transformer(), position.z) : 0.0;
        double clientX = relativeX ? clientPosition.x() + foldedX : context.nearestCopyX(position.x);
        double clientZ = relativeZ ? clientPosition.z() + foldedZ : context.nearestCopyZ(position.z);
        clientPosition.set(clientX, clientZ);

        Vec3 sentPosition = new Vec3(relativeX ? foldedX : clientX, position.y, relativeZ ? foldedZ : clientZ);
        return new ClientboundPlayerPositionPacket(
                packet.id(),
                new PositionMoveRotation(sentPosition, change.deltaMovement(), change.yRot(), change.xRot()),
                packet.relatives());
    }

    private static ClientboundAddEntityPacket addEntity(ClientboundAddEntityPacket packet, TranslationContext context) {
        Vec3 clientPos = context.toClient(
                new Vec3(packet.getX(), packet.getY(), packet.getZ()), context.trackedReach());
        return new ClientboundAddEntityPacket(
                packet.getId(), packet.getUUID(),
                clientPos.x, clientPos.y, clientPos.z,
                packet.getXRot(), packet.getYRot(), packet.getType(), packet.getData(),
                packet.getMovement(), packet.getYHeadRot());
    }

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

    private static Packet<?> moveVehicle(ClientboundMoveVehiclePacket packet, TranslationContext context) {
        Vec3 clientPos = context.toClient(packet.position(), context.trackedReach());
        return new ClientboundMoveVehiclePacket(clientPos, packet.yRot(), packet.xRot());
    }

    private static PositionMoveRotation toClientChange(TranslationContext context, PositionMoveRotation change, Set<Relative> relatives) {
        PacketReach reach = context.trackedReach();
        Vec3 position = change.position();
        Vec3 clientPos = relatives.contains(Relative.X) || relatives.contains(Relative.Z)
                ? new Vec3(
                        relatives.contains(Relative.X) ? position.x : context.toClientX(position.x, reach),
                        position.y,
                        relatives.contains(Relative.Z) ? position.z : context.toClientZ(position.z, reach))
                : context.toClient(position, reach);
        return new PositionMoveRotation(clientPos, change.deltaMovement(), change.yRot(), change.xRot());
    }

    private static ClientboundBlockUpdatePacket blockUpdate(ClientboundBlockUpdatePacket packet, TranslationContext context) {
        return new ClientboundBlockUpdatePacket(toClientBlock(context, packet.getPos()), packet.getBlockState());
    }

    private static ClientboundSectionBlocksUpdatePacket sectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, TranslationContext context) {
        return rewritePosition(
                (sectionPacket, output) -> ((SectionBlocksUpdatePacketAccessor) sectionPacket).toroidal$write(output),
                SectionBlocksUpdatePacketAccessor::toroidal$create,
                SectionPos.STREAM_CODEC, packet, context,
                section -> SectionPos.of(context.toClient(section.chunk()), section.y()));
    }

    private static ClientboundBlockEntityDataPacket blockEntityData(ClientboundBlockEntityDataPacket packet, TranslationContext context) {
        return BlockEntityDataPacketAccessor.toroidal$create(
                toClientBlock(context, packet.getPos()), packet.getType(), packet.getTag());
    }

    private static ClientboundBlockDestructionPacket blockDestruction(ClientboundBlockDestructionPacket packet, TranslationContext context) {
        return new ClientboundBlockDestructionPacket(
                packet.getId(), toClientBlock(context, packet.getPos()), packet.getProgress());
    }

    private static ClientboundLevelEventPacket levelEvent(ClientboundLevelEventPacket packet, TranslationContext context) {
        return new ClientboundLevelEventPacket(
                packet.getType(), toClientBlock(context, packet.getPos()), packet.getData(), packet.isGlobalEvent());
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
                toClientBlock(context, packet.getPos()), packet.getBlock(), packet.getB0(), packet.getB1());
    }

    private static ClientboundOpenSignEditorPacket openSignEditor(ClientboundOpenSignEditorPacket packet, TranslationContext context) {
        return new ClientboundOpenSignEditorPacket(toClientBlock(context, packet.getPos()), packet.isFrontText());
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
                packet.isOverrideLimiter(), packet.alwaysShow(),
                clientOrigin.x, clientOrigin.y, clientOrigin.z,
                packet.getXDist(), packet.getYDist(), packet.getZDist(), packet.getMaxSpeed(), packet.getCount());
    }

    private static ClientboundExplodePacket explode(ClientboundExplodePacket packet, TranslationContext context) {
        Vec3 clientCenter = context.toClient(packet.center(), PacketReach.EXPLOSION);
        return new ClientboundExplodePacket(
                clientCenter, packet.radius(), packet.blockCount(), packet.playerKnockback(),
                toClientParticle(context, packet.explosionParticle(), clientCenter),
                packet.explosionSound(),
                toClientBlockParticles(context, packet.blockParticles(), clientCenter));
    }

    private static ParticleOptions toClientParticle(TranslationContext context, ParticleOptions particle,
            Vec3 clientOrigin) {
        switch (particle) {
            case TrailParticleOption trail -> {
                return new TrailParticleOption(
                        context.transformer().nearestCopy(clientOrigin, trail.target()),
                        trail.color(), trail.duration());
            }
            case VibrationParticleOption vibration -> {
                if (!(vibration.getDestination() instanceof BlockPositionSource destination)) {
                    return particle;
                }

                BlockPos clientDestination = nearestCopyBlock(context, clientOrigin, destination.pos());
                return new VibrationParticleOption(
                        new BlockPositionSource(clientDestination), vibration.getArrivalInTicks());
            }
            default -> {
                ParticleRewriter<ParticleOptions> particleRewriter = particleRewriterFor(particle);
                return particleRewriter == null ? particle : particleRewriter.rewrite(particle, context, clientOrigin);
            }
        }
    }

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

    private static ClientboundTrackedWaypointPacket trackedWaypoint(ClientboundTrackedWaypointPacket packet, TranslationContext context) {
        if (packet.waypoint() instanceof Vec3iWaypointAccessor waypoint) {
            waypoint.toroidal$setVector(nearestCopyBlock(context, new BlockPos(waypoint.toroidal$getVector())));
        } else if (packet.waypoint() instanceof ChunkWaypointAccessor waypoint) {
            waypoint.toroidal$setChunkPos(context.nearestCopy(waypoint.toroidal$getChunkPos()));
        }

        return packet;
    }

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

    private static ServerboundInteractPacket interact(ServerboundInteractPacket packet, TranslationContext context) {
        Vec3 targetPosition = context.entityPosition().apply(packet.entityId());
        Vec3 location = packet.location();

        Vec3 serverLocation = targetPosition == null
                ? context.toServer(location)
                : context.transformer().nearestCopy(targetPosition, location);
        return new ServerboundInteractPacket(
                packet.entityId(), packet.hand(), serverLocation, packet.usingSecondaryAction());
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

    static BlockPos toClientBlock(TranslationContext context, BlockPos pos) {
        return context.transformer().reseat(pos, context.toClient(ChunkPos.containing(pos)));
    }

    private static BlockPos nearestCopyBlock(TranslationContext context, BlockPos pos) {
        return nearestCopyBlock(context.transformer(), context.clientPosition().chunk(), pos);
    }

    private static BlockPos nearestCopyBlock(TranslationContext context, Vec3 anchor, BlockPos pos) {
        return context.transformer().nearestCopy(BlockPos.containing(anchor), pos);
    }

    static BlockPos nearestCopyBlock(WorldFold transformer, ChunkPos anchor, BlockPos pos) {
        return transformer.reseat(pos, transformer.nearestCopy(anchor, ChunkPos.containing(pos)));
    }

    private static <T, P> T rewritePosition(BiConsumer<T, RegistryFriendlyByteBuf> writer,
            Function<FriendlyByteBuf, T> reader,
            StreamCodec<? super RegistryFriendlyByteBuf, P> positionCodec, T packet, TranslationContext context,
            UnaryOperator<P> toClient) {
        RegistryFriendlyByteBuf source = buffer(context);
        writer.accept(packet, source);
        P serverPosition = positionCodec.decode(source);

        RegistryFriendlyByteBuf target = buffer(context, source.readerIndex() + source.readableBytes());
        positionCodec.encode(target, toClient.apply(serverPosition));
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
