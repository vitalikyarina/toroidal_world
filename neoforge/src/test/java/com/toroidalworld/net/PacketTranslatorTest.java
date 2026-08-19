package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.mixin.ChunkWaypointAccessor;
import com.toroidalworld.mixin.PlayerLookAtPacketAccessor;
import com.toroidalworld.mixin.Vec3iWaypointAccessor;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.player.ClientPosition.BorderCenter;

import io.netty.buffer.Unpooled;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.Waypoint;

class PacketTranslatorTest {
    private static final WorldLoopTransformer TRANSFORMER =
            new WorldLoopTransformer(new WorldLoopBounds(-32, 32, -32, 32));
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final double MIRROR_X = 580.0;
    private static final double MIRROR_Z = -700.0;

    private static final BlockPos SERVER_BLOCK = new BlockPos(-510, 64, -505);
    private static final BlockPos CLIENT_BLOCK = new BlockPos(514, 64, -505);
    private static final BlockPos SERVER_CARRIED_BLOCK = new BlockPos(-510, 64, -100);
    private static final BlockPos CLIENT_CARRIED_BLOCK = new BlockPos(514, 64, -100);
    private static final ChunkPos SERVER_CHUNK = new ChunkPos(-32, -32);
    private static final ChunkPos CLIENT_CHUNK = new ChunkPos(32, -32);

    private static final double SERVER_X = -500.5;
    private static final double CLIENT_X = 523.5;
    private static final double SERVER_Z = 500.0;
    private static final double CLIENT_Z = -524.0;

    private static TranslationContext context() {
        return context(entityId -> false, entityId -> null);
    }

    private static final int VIEW_DISTANCE = 16;

    private static final IntFunction<RegistryFriendlyByteBuf> BUFFERS =
            capacity -> new RegistryFriendlyByteBuf(Unpooled.buffer(capacity), REGISTRIES);

    private static TranslationContext context(IntPredicate ownVehicle, IntFunction<Vec3> entityPosition) {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, TRANSFORMER);
        return new TranslationContext(TRANSFORMER, mirror, REGISTRIES, BUFFERS, Level.OVERWORLD,
                VIEW_DISTANCE, VIEW_DISTANCE, ownVehicle, entityPosition, () -> {});
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    }

    private static void writeEmptyLightData(FriendlyByteBuf buf) {
        buf.writeBitSet(new BitSet());
        buf.writeBitSet(new BitSet());
        buf.writeBitSet(new BitSet());
        buf.writeBitSet(new BitSet());
        buf.writeVarInt(0);
        buf.writeVarInt(0);
    }

    private static Set<ChunkPos> forgetPositions(Packet<?> packet) {
        if (packet instanceof ClientboundForgetLevelChunkPacket forget) {
            return Set.of(forget.pos());
        }

        Set<ChunkPos> positions = new HashSet<>();
        for (Packet<?> sub : ((ClientboundBundlePacket) packet).subPackets()) {
            positions.add(((ClientboundForgetLevelChunkPacket) sub).pos());
        }

        return positions;
    }

    @Nested
    class BlockPackets {
        @Test
        void blockUpdateMovesToTheHeldCopy() {
            BlockState state = Blocks.STONE.defaultBlockState();
            ClientboundBlockUpdatePacket translated = (ClientboundBlockUpdatePacket) PacketTranslator.toClient(
                    new ClientboundBlockUpdatePacket(SERVER_BLOCK, state), context());

            assertEquals(CLIENT_BLOCK, translated.getPos());
            assertSame(state, translated.getBlockState());
        }

        @Test
        void sectionBlocksUpdateMovesTheSectionKeepsTheBlocks() {
            short packed = 1234;
            BlockState state = Blocks.STONE.defaultBlockState();
            RegistryFriendlyByteBuf buf = buffer();
            SectionPos.STREAM_CODEC.encode(buf, SectionPos.of(SERVER_CHUNK, 4));
            buf.writeVarInt(1);
            buf.writeVarLong((long) Block.getId(state) << 12 | packed);
            ClientboundSectionBlocksUpdatePacket packet = ClientboundSectionBlocksUpdatePacket.STREAM_CODEC.decode(buf);

            ClientboundSectionBlocksUpdatePacket translated =
                    (ClientboundSectionBlocksUpdatePacket) PacketTranslator.toClient(packet, context());

            List<BlockPos> positions = new ArrayList<>();
            List<BlockState> states = new ArrayList<>();
            translated.runUpdates((pos, blockState) -> {
                positions.add(pos.immutable());
                states.add(blockState);
            });

            assertEquals(1, positions.size());
            BlockPos pos = positions.getFirst();
            assertEquals(CLIENT_CHUNK.x(), SectionPos.blockToSectionCoord(pos.getX()));
            assertEquals(CLIENT_CHUNK.z(), SectionPos.blockToSectionCoord(pos.getZ()));
            assertEquals(SectionPos.sectionRelativeX(packed), pos.getX() & 15);
            assertEquals(SectionPos.sectionRelativeZ(packed), pos.getZ() & 15);
            assertEquals(4 * 16 + SectionPos.sectionRelativeY(packed), pos.getY());
            assertSame(state, states.getFirst());
        }

        @Test
        void blockEntityDataMovesThePositionKeepsTypeAndTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Loot", 7);
            RegistryFriendlyByteBuf buf = buffer();
            BlockPos.STREAM_CODEC.encode(buf, SERVER_BLOCK);
            ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).encode(buf, BlockEntityType.CHEST);
            ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(buf, tag);
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.STREAM_CODEC.decode(buf);

            ClientboundBlockEntityDataPacket translated =
                    (ClientboundBlockEntityDataPacket) PacketTranslator.toClient(packet, context());

            assertEquals(CLIENT_BLOCK, translated.getPos());
            assertSame(BlockEntityType.CHEST, translated.getType());
            assertEquals(tag, translated.getTag());
        }

        @Test
        void blockEventKeepsItsPayload() {
            ClientboundBlockEventPacket translated = (ClientboundBlockEventPacket) PacketTranslator.toClient(
                    new ClientboundBlockEventPacket(SERVER_BLOCK, Blocks.NOTE_BLOCK, 1, 2), context());

            assertEquals(CLIENT_BLOCK, translated.getPos());
            assertSame(Blocks.NOTE_BLOCK, translated.getBlock());
            assertEquals(1, translated.getB0());
            assertEquals(2, translated.getB1());
        }

        @Test
        void blockDestructionKeepsIdAndProgress() {
            ClientboundBlockDestructionPacket translated = (ClientboundBlockDestructionPacket) PacketTranslator.toClient(
                    new ClientboundBlockDestructionPacket(9, SERVER_BLOCK, 5), context());

            assertEquals(CLIENT_BLOCK, translated.getPos());
            assertEquals(9, translated.getId());
            assertEquals(5, translated.getProgress());
        }
    }

    @Nested
    class ChunkPackets {
        @Test
        void levelChunkMovesThePositionKeepsTheBlob() {
            byte[] blob = {1, 2, 3};
            RegistryFriendlyByteBuf buf = buffer();
            buf.writeInt(SERVER_CHUNK.x());
            buf.writeInt(SERVER_CHUNK.z());
            buf.writeVarInt(0);
            buf.writeVarInt(blob.length);
            buf.writeBytes(blob);
            buf.writeVarInt(0);
            writeEmptyLightData(buf);
            ClientboundLevelChunkWithLightPacket packet = ClientboundLevelChunkWithLightPacket.STREAM_CODEC.decode(buf);

            ClientboundLevelChunkWithLightPacket translated =
                    (ClientboundLevelChunkWithLightPacket) PacketTranslator.toClient(packet, context());

            assertSame(packet, translated);
            assertEquals(CLIENT_CHUNK.x(), translated.getX());
            assertEquals(CLIENT_CHUNK.z(), translated.getZ());
            assertTrue(translated.getChunkData().getHeightmaps().isEmpty());
            FriendlyByteBuf data = translated.getChunkData().getReadBuffer();
            assertEquals(blob.length, data.readableBytes());
            byte[] readBack = new byte[blob.length];
            data.readBytes(readBack);
            assertArrayEquals(blob, readBack);
        }

        @Test
        void lightUpdateMovesThePosition() {
            RegistryFriendlyByteBuf buf = buffer();
            buf.writeVarInt(SERVER_CHUNK.x());
            buf.writeVarInt(SERVER_CHUNK.z());
            writeEmptyLightData(buf);
            ClientboundLightUpdatePacket packet = ClientboundLightUpdatePacket.STREAM_CODEC.decode(buf);

            ClientboundLightUpdatePacket translated =
                    (ClientboundLightUpdatePacket) PacketTranslator.toClient(packet, context());

            assertSame(packet, translated);
            assertEquals(CLIENT_CHUNK.x(), translated.getX());
            assertEquals(CLIENT_CHUNK.z(), translated.getZ());
            assertTrue(translated.getLightData().getSkyUpdates().isEmpty());
            assertTrue(translated.getLightData().getBlockUpdates().isEmpty());
        }

        @Test
        void forgetChunkFollowsTheHeldCopy() {
            ClientboundForgetLevelChunkPacket translated = (ClientboundForgetLevelChunkPacket) PacketTranslator.toClient(
                    new ClientboundForgetLevelChunkPacket(SERVER_CHUNK), context());

            assertEquals(CLIENT_CHUNK, translated.pos());
        }

        @Test
        void antipodalForgetSplitsIntoBothCopies() {
            Packet<?> translated = PacketTranslator.toClient(
                    new ClientboundForgetLevelChunkPacket(new ChunkPos(4, -44)), context());

            assertEquals(Set.of(new ChunkPos(68, -44), new ChunkPos(4, -44)), forgetPositions(translated));
        }

        @Test
        void forgetAmbiguousOnBothAxesFansOutToFourCopies() {
            Packet<?> translated = PacketTranslator.toClient(
                    new ClientboundForgetLevelChunkPacket(new ChunkPos(4, -12)), context());

            assertEquals(Set.of(
                    new ChunkPos(68, -76), new ChunkPos(4, -76),
                    new ChunkPos(68, -12), new ChunkPos(4, -12)), forgetPositions(translated));
        }

        @Test
        void unboundedAxisNeverSplitsHoweverFarTheForget() {
            WorldLoopTransformer singleAxis = new WorldLoopTransformer(new WorldLoopBounds(
                    new WorldLoopBounds.AxisBounds.Looped(-32, 32),
                    WorldLoopBounds.AxisBounds.Unbounded.INSTANCE));
            ClientPosition mirror = new ClientPosition();
            mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, singleAxis);
            TranslationContext context = new TranslationContext(singleAxis, mirror, REGISTRIES, BUFFERS,
                    Level.OVERWORLD, VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityId -> null, () -> {});

            ClientboundForgetLevelChunkPacket translated = (ClientboundForgetLevelChunkPacket) PacketTranslator.toClient(
                    new ClientboundForgetLevelChunkPacket(new ChunkPos(0, -100)), context);

            assertEquals(new ChunkPos(64, -100), translated.pos());
        }

        @Test
        void chunkCacheCenterFollowsTheMirror() {
            ClientboundSetChunkCacheCenterPacket translated =
                    (ClientboundSetChunkCacheCenterPacket) PacketTranslator.toClient(
                            new ClientboundSetChunkCacheCenterPacket(SERVER_CHUNK.x(), SERVER_CHUNK.z()), context());

            assertEquals(CLIENT_CHUNK.x(), translated.getX());
            assertEquals(CLIENT_CHUNK.z(), translated.getZ());
        }
    }

    @Nested
    class ChunkAnchor {
        private static final ChunkPos MIRROR_CHUNK = new ChunkPos(36, -44);

        private static final ChunkPos HELD_CENTER = new ChunkPos(8, -44);

        private static final ChunkPos DISPUTED_CHUNK = new ChunkPos(0, -44);
        private static final BlockPos DISPUTED_BLOCK = new BlockPos(5, 64, -700);
        private static final BlockPos MIRROR_ANCHORED_BLOCK = new BlockPos(1029, 64, -700);

        private static TranslationContext contextWith(ChunkPos heldCacheCenter) {
            ClientPosition mirror = new ClientPosition();
            mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, TRANSFORMER);
            if (heldCacheCenter != null) {
                mirror.setHeldCacheCenter(heldCacheCenter);
            }

            return new TranslationContext(TRANSFORMER, mirror, REGISTRIES, BUFFERS, Level.OVERWORLD,
                    VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityId -> null, () -> {});
        }

        @Test
        void chunkTrafficFollowsTheHeldCacheCenter() {
            ClientboundBlockUpdatePacket translated = (ClientboundBlockUpdatePacket) PacketTranslator.toClient(
                    new ClientboundBlockUpdatePacket(DISPUTED_BLOCK, Blocks.STONE.defaultBlockState()),
                    contextWith(HELD_CENTER));

            assertEquals(DISPUTED_BLOCK, translated.getPos());
        }

        @Test
        void chunkTrafficFallsBackToTheMirrorBeforeTheFirstCacheCenter() {
            ClientboundBlockUpdatePacket translated = (ClientboundBlockUpdatePacket) PacketTranslator.toClient(
                    new ClientboundBlockUpdatePacket(DISPUTED_BLOCK, Blocks.STONE.defaultBlockState()),
                    contextWith(null));

            assertEquals(MIRROR_ANCHORED_BLOCK, translated.getPos());
        }

        @Test
        void theCacheCenterPacketMovesTheAnchorForWhatFollows() {
            TranslationContext context = contextWith(null);
            PacketTranslator.toClient(
                    new ClientboundSetChunkCacheCenterPacket(HELD_CENTER.x(), HELD_CENTER.z()), context);

            ClientboundBlockUpdatePacket translated = (ClientboundBlockUpdatePacket) PacketTranslator.toClient(
                    new ClientboundBlockUpdatePacket(DISPUTED_BLOCK, Blocks.STONE.defaultBlockState()), context);

            assertEquals(DISPUTED_BLOCK, translated.getPos());
        }

        @Test
        void theCacheCenterPacketFoldsAroundTheMirrorNotTheStaleCenter() {
            ClientboundSetChunkCacheCenterPacket translated =
                    (ClientboundSetChunkCacheCenterPacket) PacketTranslator.toClient(
                            new ClientboundSetChunkCacheCenterPacket(-20, MIRROR_CHUNK.z()),
                            contextWith(DISPUTED_CHUNK));

            assertEquals(44, translated.getX());
            assertEquals(MIRROR_CHUNK.z(), translated.getZ());
        }

        @Test
        void rebaseClearsTheHeldCacheCenter() {
            ClientPosition mirror = new ClientPosition();
            mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, TRANSFORMER);
            mirror.setHeldCacheCenter(HELD_CENTER);

            mirror.rebase(MIRROR_X, MIRROR_Z, Level.NETHER, TRANSFORMER);

            assertNull(mirror.heldCacheCenter());
        }
    }

    @Nested
    class WaypointPackets {
        @Test
        void blockWaypointMovesToTheHeldCopy() {
            ClientboundTrackedWaypointPacket translated = (ClientboundTrackedWaypointPacket) PacketTranslator.toClient(
                    ClientboundTrackedWaypointPacket.addWaypointPosition(new UUID(1L, 2L), new Waypoint.Icon(), SERVER_BLOCK),
                    context());

            assertEquals(CLIENT_BLOCK, new BlockPos(((Vec3iWaypointAccessor) translated.waypoint()).toroidal$getVector()));
        }

        @Test
        void chunkWaypointFollowsTheHeldCopy() {
            ClientboundTrackedWaypointPacket translated = (ClientboundTrackedWaypointPacket) PacketTranslator.toClient(
                    ClientboundTrackedWaypointPacket.addWaypointChunk(new UUID(1L, 2L), new Waypoint.Icon(), SERVER_CHUNK),
                    context());

            assertEquals(CLIENT_CHUNK, ((ChunkWaypointAccessor) translated.waypoint()).toroidal$getChunkPos());
        }

        @Test
        void azimuthWaypointPassesThrough() {
            ClientboundTrackedWaypointPacket packet =
                    ClientboundTrackedWaypointPacket.addWaypointAzimuth(new UUID(1L, 2L), new Waypoint.Icon(), 1.5F);

            assertSame(packet, PacketTranslator.toClient(packet, context()));
        }
    }

    @Nested
    class BorderPackets {
        private static final double OLD_SIZE = 3000.0;
        private static final double NEW_SIZE = 1500.0;
        private static final long LERP_TIME = 300000L;
        private static final int ABSOLUTE_MAX_SIZE = 29999984;
        private static final int WARNING_BLOCKS = 7;
        private static final int WARNING_TIME = 15;

        private static ClientboundInitializeBorderPacket initializePacket() {
            RegistryFriendlyByteBuf buf = buffer();
            buf.writeDouble(SERVER_X);
            buf.writeDouble(SERVER_Z);
            buf.writeDouble(OLD_SIZE);
            buf.writeDouble(NEW_SIZE);
            buf.writeVarLong(LERP_TIME);
            buf.writeVarInt(ABSOLUTE_MAX_SIZE);
            buf.writeVarInt(WARNING_BLOCKS);
            buf.writeVarInt(WARNING_TIME);
            return ClientboundInitializeBorderPacket.STREAM_CODEC.decode(buf);
        }

        private static ClientboundSetBorderCenterPacket centerPacket() {
            RegistryFriendlyByteBuf buf = buffer();
            buf.writeDouble(SERVER_X);
            buf.writeDouble(SERVER_Z);
            return ClientboundSetBorderCenterPacket.STREAM_CODEC.decode(buf);
        }

        @Test
        void initializeBorderMovesTheCentreToTheNearestCopy() {
            TranslationContext context = context();
            ClientboundInitializeBorderPacket translated =
                    (ClientboundInitializeBorderPacket) PacketTranslator.toClient(initializePacket(), context);

            assertEquals(CLIENT_X, translated.getNewCenterX());
            assertEquals(CLIENT_Z, translated.getNewCenterZ());
            assertEquals(new BorderCenter(CLIENT_X, CLIENT_Z), context.clientPosition().heldBorderCenter());
        }

        @Test
        void initializeBorderKeepsEverythingAfterTheCentre() {
            ClientboundInitializeBorderPacket translated =
                    (ClientboundInitializeBorderPacket) PacketTranslator.toClient(initializePacket(), context());

            assertEquals(OLD_SIZE, translated.getOldSize());
            assertEquals(NEW_SIZE, translated.getNewSize());
            assertEquals(LERP_TIME, translated.getLerpTime());
            assertEquals(ABSOLUTE_MAX_SIZE, translated.getNewAbsoluteMaxSize());
            assertEquals(WARNING_BLOCKS, translated.getWarningBlocks());
            assertEquals(WARNING_TIME, translated.getWarningTime());
        }

        @Test
        void setBorderCenterMovesTheCentreToTheNearestCopy() {
            TranslationContext context = context();
            ClientboundSetBorderCenterPacket translated =
                    (ClientboundSetBorderCenterPacket) PacketTranslator.toClient(centerPacket(), context);

            assertEquals(CLIENT_X, translated.getNewCenterX());
            assertEquals(CLIENT_Z, translated.getNewCenterZ());
            assertEquals(new BorderCenter(CLIENT_X, CLIENT_Z), context.clientPosition().heldBorderCenter());
        }

        @Test
        void unseededMirrorPassesThrough() {
            ClientPosition mirror = new ClientPosition();
            TranslationContext context = new TranslationContext(TRANSFORMER, mirror, REGISTRIES, BUFFERS,
                    Level.OVERWORLD, VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityId -> null, () -> {});
            ClientboundSetBorderCenterPacket packet = centerPacket();

            assertSame(packet, PacketTranslator.toClient(packet, context));
            assertNull(context.clientPosition().heldBorderCenter());
        }
    }

    @Nested
    class DirectionalHintPackets {
        @Test
        void spawnPositionMovesToTheNearestCopy() {
            TranslationContext context = context();
            ClientboundSetDefaultSpawnPositionPacket translated =
                    (ClientboundSetDefaultSpawnPositionPacket) PacketTranslator.toClient(
                            new ClientboundSetDefaultSpawnPositionPacket(
                                    LevelData.RespawnData.of(Level.OVERWORLD, SERVER_BLOCK, 30.0F, 10.0F)),
                            context);

            assertEquals(CLIENT_BLOCK, translated.respawnData().pos());
            assertEquals(Level.OVERWORLD, translated.respawnData().dimension());
            assertEquals(30.0F, translated.respawnData().yaw());
            assertEquals(10.0F, translated.respawnData().pitch());
            assertEquals(CLIENT_BLOCK, context.clientPosition().heldSpawn());
        }

        @Test
        void foreignDimensionSpawnPassesThrough() {
            ClientboundSetDefaultSpawnPositionPacket packet = new ClientboundSetDefaultSpawnPositionPacket(
                    LevelData.RespawnData.of(Level.NETHER, SERVER_BLOCK, 0.0F, 0.0F));

            assertSame(packet, PacketTranslator.toClient(packet, context()));
        }

        @Test
        void lookAtTargetMovesToTheNearestCopy() {
            ClientboundPlayerLookAtPacket translated = (ClientboundPlayerLookAtPacket) PacketTranslator.toClient(
                    new ClientboundPlayerLookAtPacket(EntityAnchorArgument.Anchor.EYES, SERVER_X, 70.0, SERVER_Z),
                    context());

            PlayerLookAtPacketAccessor accessor = (PlayerLookAtPacketAccessor) translated;
            assertEquals(CLIENT_X, accessor.toroidal$getX());
            assertEquals(CLIENT_Z, accessor.toroidal$getZ());
            assertSame(EntityAnchorArgument.Anchor.EYES, translated.getFromAnchor());
        }

        @Test
        void damageSourcePositionMovesToTheNearestCopy() {
            Holder<DamageType> sourceType = Holder.direct(new DamageType("generic", 0.0F));
            ClientboundDamageEventPacket translated = (ClientboundDamageEventPacket) PacketTranslator.toClient(
                    new ClientboundDamageEventPacket(3, sourceType, -1, -1,
                            Optional.of(new Vec3(SERVER_X, 70.0, SERVER_Z))),
                    context());

            assertEquals(Optional.of(new Vec3(CLIENT_X, 70.0, CLIENT_Z)), translated.sourcePosition());
            assertSame(sourceType, translated.sourceType());
            assertEquals(3, translated.entityId());
        }

        @Test
        void entitySourcedDamagePassesThrough() {
            ClientboundDamageEventPacket packet = new ClientboundDamageEventPacket(
                    3, Holder.direct(new DamageType("generic", 0.0F)), 5, 6, Optional.empty());

            assertSame(packet, PacketTranslator.toClient(packet, context()));
        }

        @Test
        void minecartLerpStepsMoveToTheHeldCopy() {
            NewMinecartBehavior.MinecartStep step = new NewMinecartBehavior.MinecartStep(
                    new Vec3(SERVER_X, 70.0, SERVER_Z), new Vec3(0.1, 0.0, 0.2), 30.0F, 10.0F, 1.0F);

            ClientboundMoveMinecartPacket translated = (ClientboundMoveMinecartPacket) PacketTranslator.toClient(
                    new ClientboundMoveMinecartPacket(9, List.of(step)), context());

            NewMinecartBehavior.MinecartStep translatedStep = translated.lerpSteps().getFirst();
            assertEquals(new Vec3(CLIENT_X, 70.0, CLIENT_Z), translatedStep.position());
            assertEquals(new Vec3(0.1, 0.0, 0.2), translatedStep.movement());
            assertEquals(30.0F, translatedStep.yRot());
            assertEquals(10.0F, translatedStep.xRot());
            assertEquals(1.0F, translatedStep.weight());
            assertEquals(9, translated.entityId());
        }
    }

    @Nested
    class ParticlePayloads {
        @Test
        void trailTargetFollowsTheTranslatedStart() {
            Vec3 serverTarget = new Vec3(SERVER_X + 3.0, 65.0, SERVER_Z + 3.0);
            ClientboundLevelParticlesPacket translated = (ClientboundLevelParticlesPacket) PacketTranslator.toClient(
                    new ClientboundLevelParticlesPacket(
                            new TrailParticleOption(serverTarget, 16545810, 30), true, true,
                            SERVER_X, 64.0, SERVER_Z, 0.0F, 0.0F, 0.0F, 0.0F, 1),
                    context());

            TrailParticleOption trail = (TrailParticleOption) translated.getParticle();
            assertEquals(new Vec3(CLIENT_X + 3.0, 65.0, CLIENT_Z + 3.0), trail.target());
            assertEquals(CLIENT_X, translated.getX());
            assertEquals(CLIENT_Z, translated.getZ());
            assertEquals(16545810, trail.color());
            assertEquals(30, trail.duration());
        }

        @Test
        void vibrationBlockDestinationFollowsTheTranslatedStart() {
            ClientboundLevelParticlesPacket translated = (ClientboundLevelParticlesPacket) PacketTranslator.toClient(
                    new ClientboundLevelParticlesPacket(
                            new VibrationParticleOption(new BlockPositionSource(SERVER_BLOCK), 12), false, false,
                            SERVER_X, 64.0, SERVER_Z, 0.0F, 0.0F, 0.0F, 0.0F, 1),
                    context());

            VibrationParticleOption vibration = (VibrationParticleOption) translated.getParticle();
            assertEquals(CLIENT_BLOCK, ((BlockPositionSource) vibration.getDestination()).pos());
            assertEquals(12, vibration.getArrivalInTicks());
        }

        @Test
        void vibrationEntityDestinationPassesThrough() {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeVarInt(7);
            buf.writeFloat(0.5F);
            VibrationParticleOption particle =
                    new VibrationParticleOption(EntityPositionSource.STREAM_CODEC.decode(buf), 12);

            ClientboundLevelParticlesPacket translated = (ClientboundLevelParticlesPacket) PacketTranslator.toClient(
                    new ClientboundLevelParticlesPacket(particle, false, false,
                            SERVER_X, 64.0, SERVER_Z, 0.0F, 0.0F, 0.0F, 0.0F, 1),
                    context());

            assertSame(particle, translated.getParticle());
        }

        @Test
        void blockParticlePositionMovesToTheHeldCopy() {
            BlockState state = Blocks.STONE.defaultBlockState();
            ClientboundLevelParticlesPacket translated = (ClientboundLevelParticlesPacket) PacketTranslator.toClient(
                    new ClientboundLevelParticlesPacket(
                            new BlockParticleOption(ParticleTypes.BLOCK, state, SERVER_BLOCK), false, false,
                            SERVER_X, 64.0, SERVER_Z, 0.0F, 0.0F, 0.0F, 0.0F, 1),
                    context());

            BlockParticleOption block = (BlockParticleOption) translated.getParticle();
            assertEquals(CLIENT_BLOCK, block.getPos());
            assertSame(state, block.getState());
        }

        @Test
        void positionlessPayloadPassesThrough() {
            ClientboundLevelParticlesPacket translated = (ClientboundLevelParticlesPacket) PacketTranslator.toClient(
                    new ClientboundLevelParticlesPacket(ParticleTypes.FLAME, false, false,
                            SERVER_X, 64.0, SERVER_Z, 0.0F, 0.0F, 0.0F, 0.0F, 1),
                    context());

            assertSame(ParticleTypes.FLAME, translated.getParticle());
            assertEquals(CLIENT_X, translated.getX());
        }

        @Test
        void explosionParticleFollowsTheTranslatedCentre() {
            Vec3 serverTarget = new Vec3(SERVER_X + 3.0, 71.0, SERVER_Z + 3.0);
            ClientboundExplodePacket translated = (ClientboundExplodePacket) PacketTranslator.toClient(
                    new ClientboundExplodePacket(
                            new Vec3(SERVER_X, 70.0, SERVER_Z), 3.0F, 4, Optional.empty(),
                            new TrailParticleOption(serverTarget, 6250335, 20),
                            SoundEvents.GENERIC_EXPLODE, WeightedList.of()),
                    context());

            TrailParticleOption trail = (TrailParticleOption) translated.explosionParticle();
            assertEquals(new Vec3(CLIENT_X + 3.0, 71.0, CLIENT_Z + 3.0), trail.target());
            assertEquals(new Vec3(CLIENT_X, 70.0, CLIENT_Z), translated.center());
        }

        @Test
        void explosionBlockParticlesFollowTheTranslatedCentre() {
            BlockState state = Blocks.STONE.defaultBlockState();
            WeightedList<ExplosionParticleInfo> blockParticles = WeightedList.of(new ExplosionParticleInfo(
                    new BlockParticleOption(ParticleTypes.BLOCK, state, SERVER_BLOCK), 1.5F, 0.5F));

            ClientboundExplodePacket translated = (ClientboundExplodePacket) PacketTranslator.toClient(
                    new ClientboundExplodePacket(
                            new Vec3(SERVER_X, 70.0, SERVER_Z), 3.0F, 4, Optional.empty(),
                            ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE, blockParticles),
                    context());

            ExplosionParticleInfo info = translated.blockParticles().unwrap().getFirst().value();
            assertEquals(CLIENT_BLOCK, ((BlockParticleOption) info.particle()).getPos());
            assertEquals(1.5F, info.scaling());
            assertEquals(0.5F, info.speed());
        }

        @Test
        void positionlessBlockParticlesKeepTheirList() {
            WeightedList<ExplosionParticleInfo> blockParticles =
                    WeightedList.of(new ExplosionParticleInfo(ParticleTypes.FLAME, 1.0F, 1.0F));

            ClientboundExplodePacket translated = (ClientboundExplodePacket) PacketTranslator.toClient(
                    new ClientboundExplodePacket(
                            new Vec3(SERVER_X, 70.0, SERVER_Z), 3.0F, 4, Optional.empty(),
                            ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE, blockParticles),
                    context());

            assertSame(blockParticles, translated.blockParticles());
        }
    }

    @Nested
    class PlayerPosition {
        @Test
        void relativeLapFoldsToNoMove() {
            ClientPosition mirror = new ClientPosition();
            mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, TRANSFORMER);
            TranslationContext context = new TranslationContext(TRANSFORMER, mirror, REGISTRIES, BUFFERS,
                    Level.OVERWORLD, VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityId -> null, () -> {});

            ClientboundPlayerPositionPacket translated = (ClientboundPlayerPositionPacket) PacketTranslator.toClient(
                    new ClientboundPlayerPositionPacket(1,
                            new PositionMoveRotation(new Vec3(1024.0, 0.0, 0.0), Vec3.ZERO, 0.0F, 0.0F),
                            Set.of(Relative.X, Relative.Z)),
                    context);

            assertEquals(new Vec3(0.0, 0.0, 0.0), translated.change().position());
            assertEquals(MIRROR_X, mirror.x());
            assertEquals(MIRROR_Z, mirror.z());
        }

        @Test
        void relativeDeltaFoldsThroughTheSeam() {
            ClientPosition mirror = new ClientPosition();
            mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, TRANSFORMER);
            TranslationContext context = new TranslationContext(TRANSFORMER, mirror, REGISTRIES, BUFFERS,
                    Level.OVERWORLD, VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityId -> null, () -> {});

            ClientboundPlayerPositionPacket translated = (ClientboundPlayerPositionPacket) PacketTranslator.toClient(
                    new ClientboundPlayerPositionPacket(1,
                            new PositionMoveRotation(new Vec3(1000.0, 0.0, 0.0), Vec3.ZERO, 0.0F, 0.0F),
                            Set.of(Relative.X, Relative.Z)),
                    context);

            assertEquals(-24.0, translated.change().position().x);
            assertEquals(MIRROR_X - 24.0, mirror.x());
        }
    }

    @Nested
    class EntityPackets {
        @Test
        void addEntityUnwrapsAroundTheMirror() {
            ClientboundAddEntityPacket translated = (ClientboundAddEntityPacket) PacketTranslator.toClient(
                    new ClientboundAddEntityPacket(11, new UUID(1L, 2L), SERVER_X, 70.0, SERVER_Z,
                            0.0F, 0.0F, EntityType.PIG, 0, Vec3.ZERO, 0.0),
                    context());

            assertEquals(CLIENT_X, translated.getX());
            assertEquals(70.0, translated.getY());
            assertEquals(CLIENT_Z, translated.getZ());
            assertEquals(11, translated.getId());
        }

        @Test
        void teleportOfTheOwnControlledVehicleIsDropped() {
            ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(
                    42, new PositionMoveRotation(new Vec3(SERVER_X, 70.0, SERVER_Z), Vec3.ZERO, 0.0F, 0.0F),
                    Set.of(), false);

            assertNull(PacketTranslator.toClient(packet, context(entityId -> entityId == 42, entityId -> null)));
        }

        @Test
        void teleportTranslatesAbsoluteAxesKeepsRelativeOnesRaw() {
            ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(
                    42, new PositionMoveRotation(new Vec3(5.0, 70.0, SERVER_Z), Vec3.ZERO, 30.0F, 10.0F),
                    Set.of(Relative.X), true);

            ClientboundTeleportEntityPacket translated =
                    (ClientboundTeleportEntityPacket) PacketTranslator.toClient(packet, context());

            assertEquals(new Vec3(5.0, 70.0, CLIENT_Z), translated.change().position());
            assertEquals(Set.of(Relative.X), translated.relatives());
            assertTrue(translated.onGround());
        }

        @Test
        void entityPositionSyncTranslatesTheAbsolutePosition() {
            ClientboundEntityPositionSyncPacket packet = new ClientboundEntityPositionSyncPacket(
                    7, new PositionMoveRotation(new Vec3(SERVER_X, 70.0, SERVER_Z), Vec3.ZERO, 0.0F, 0.0F), false);

            ClientboundEntityPositionSyncPacket translated =
                    (ClientboundEntityPositionSyncPacket) PacketTranslator.toClient(packet, context());

            assertEquals(new Vec3(CLIENT_X, 70.0, CLIENT_Z), translated.values().position());
        }

        @Test
        void moveVehicleTranslatesThePosition() {
            ClientboundMoveVehiclePacket translated = (ClientboundMoveVehiclePacket) PacketTranslator.toClient(
                    new ClientboundMoveVehiclePacket(new Vec3(SERVER_X, 70.0, SERVER_Z), 30.0F, 10.0F), context());

            assertEquals(new Vec3(CLIENT_X, 70.0, CLIENT_Z), translated.position());
        }

        @Test
        void synchedBlockPosFollowsTheEntityOtherValuesPassThrough() {
            ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                    new SynchedEntityData.DataValue<>(0, EntityDataSerializers.OPTIONAL_BLOCK_POS, Optional.of(SERVER_CARRIED_BLOCK)),
                    new SynchedEntityData.DataValue<>(1, EntityDataSerializers.BYTE, (byte) 6),
                    new SynchedEntityData.DataValue<>(2, EntityDataSerializers.OPTIONAL_BLOCK_POS, Optional.empty()),
                    new SynchedEntityData.DataValue<>(3, EntityDataSerializers.BLOCK_POS, SERVER_CARRIED_BLOCK)));

            ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                    packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

            assertEquals(Optional.of(CLIENT_CARRIED_BLOCK), translated.packedItems().get(0).value());
            assertEquals((byte) 6, translated.packedItems().get(1).value());
            assertEquals(Optional.empty(), translated.packedItems().get(2).value());
            assertEquals(CLIENT_CARRIED_BLOCK, translated.packedItems().get(3).value());
        }

        @Test
        void synchedGlobalPosFollowsTheEntityOnlyInItsOwnDimension() {
            GlobalPos elsewhere = GlobalPos.of(Level.NETHER, SERVER_CARRIED_BLOCK);
            ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                    new SynchedEntityData.DataValue<>(0, EntityDataSerializers.OPTIONAL_GLOBAL_POS,
                            Optional.of(GlobalPos.of(Level.OVERWORLD, SERVER_CARRIED_BLOCK))),
                    new SynchedEntityData.DataValue<>(1, EntityDataSerializers.OPTIONAL_GLOBAL_POS, Optional.of(elsewhere))));

            ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                    packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

            assertEquals(Optional.of(GlobalPos.of(Level.OVERWORLD, CLIENT_CARRIED_BLOCK)),
                    translated.packedItems().get(0).value());
            assertEquals(Optional.of(elsewhere), translated.packedItems().get(1).value());
        }

        @Test
        void synchedParticlePayloadFollowsTheEntity() {
            TrailParticleOption particle =
                    new TrailParticleOption(new Vec3(SERVER_X + 3.0, 71.0, SERVER_Z + 3.0), 16545810, 30);
            ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                    new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLE, particle)));

            ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                    packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

            TrailParticleOption trail = (TrailParticleOption) translated.packedItems().getFirst().value();
            assertEquals(new Vec3(CLIENT_X + 3.0, 71.0, CLIENT_Z + 3.0), trail.target());
            assertEquals(16545810, trail.color());
            assertEquals(30, trail.duration());
        }

        @Test
        void synchedParticleListFollowsTheEntity() {
            TrailParticleOption particle =
                    new TrailParticleOption(new Vec3(SERVER_X + 3.0, 71.0, SERVER_Z + 3.0), 16545810, 30);
            ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                    new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLES,
                            List.<ParticleOptions>of(ParticleTypes.FLAME, particle))));

            ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                    packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

            List<?> particles = (List<?>) translated.packedItems().getFirst().value();
            assertSame(ParticleTypes.FLAME, particles.get(0));
            assertEquals(new Vec3(CLIENT_X + 3.0, 71.0, CLIENT_Z + 3.0), ((TrailParticleOption) particles.get(1)).target());
        }

        @Test
        void synchedParticleWithoutTheEntityPassesThrough() {
            TrailParticleOption particle =
                    new TrailParticleOption(new Vec3(SERVER_X + 3.0, 71.0, SERVER_Z + 3.0), 16545810, 30);
            ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                    new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLE, particle)));

            ClientboundSetEntityDataPacket translated =
                    (ClientboundSetEntityDataPacket) PacketTranslator.toClient(packet, context());

            assertSame(particle, translated.packedItems().getFirst().value());
        }

        @Test
        void positionlessSynchedParticleKeepsItsValue() {
            ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                    new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLE, ParticleTypes.FLAME)));

            ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                    packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

            assertSame(ParticleTypes.FLAME, translated.packedItems().getFirst().value());
        }
    }

    @Nested
    class ServerboundRoundTrip {
        @Test
        void playerActionReturnsToTheServerFrame() {
            ServerboundPlayerActionPacket translated = (ServerboundPlayerActionPacket) PacketTranslator.toServer(
                    new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, CLIENT_BLOCK, Direction.UP, 3),
                    context());

            assertEquals(SERVER_BLOCK, translated.getPos());
            assertEquals(3, translated.getSequence());
        }

        @Test
        void blockUpdateThenPlayerActionRoundTrips() {
            ClientboundBlockUpdatePacket sent = (ClientboundBlockUpdatePacket) PacketTranslator.toClient(
                    new ClientboundBlockUpdatePacket(SERVER_BLOCK, Blocks.STONE.defaultBlockState()), context());

            ServerboundPlayerActionPacket returned = (ServerboundPlayerActionPacket) PacketTranslator.toServer(
                    new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, sent.getPos(), Direction.UP, 0),
                    context());

            assertEquals(SERVER_BLOCK, returned.getPos());
        }

        @Test
        void useItemOnCarriesTheHitOffsetWithTheBlock() {
            Vec3 location = Vec3.atLowerCornerOf(CLIENT_BLOCK).add(0.3, 0.5, 1.0);
            BlockHitResult hit = new BlockHitResult(location, Direction.SOUTH, CLIENT_BLOCK, false);

            ServerboundUseItemOnPacket translated = (ServerboundUseItemOnPacket) PacketTranslator.toServer(
                    new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 4), context());

            BlockHitResult translatedHit = translated.getHitResult();
            assertEquals(SERVER_BLOCK, translatedHit.getBlockPos());
            Vec3 expected = Vec3.atLowerCornerOf(SERVER_BLOCK).add(0.3, 0.5, 1.0);
            assertEquals(expected.x, translatedHit.getLocation().x, 1.0e-9);
            assertEquals(expected.y, translatedHit.getLocation().y, 1.0e-9);
            assertEquals(expected.z, translatedHit.getLocation().z, 1.0e-9);
            assertEquals(Direction.SOUTH, translatedHit.getDirection());
        }

        @Test
        void pickItemFromBlockReturnsToTheServerFrame() {
            ServerboundPickItemFromBlockPacket translated = (ServerboundPickItemFromBlockPacket) PacketTranslator.toServer(
                    new ServerboundPickItemFromBlockPacket(CLIENT_BLOCK, true), context());

            assertEquals(SERVER_BLOCK, translated.pos());
        }

        @Test
        void signUpdateReturnsToTheServerFrame() {
            ServerboundSignUpdatePacket translated = (ServerboundSignUpdatePacket) PacketTranslator.toServer(
                    new ServerboundSignUpdatePacket(CLIENT_BLOCK, true, "a", "b", "c", "d"), context());

            assertEquals(SERVER_BLOCK, translated.getPos());
            assertArrayEquals(new String[] {"a", "b", "c", "d"}, translated.getLines());
        }

        @Test
        void blockEntityTagQueryReturnsToTheServerFrame() {
            ServerboundBlockEntityTagQueryPacket translated =
                    (ServerboundBlockEntityTagQueryPacket) PacketTranslator.toServer(
                            new ServerboundBlockEntityTagQueryPacket(8, CLIENT_BLOCK), context());

            assertEquals(SERVER_BLOCK, translated.getPos());
            assertEquals(8, translated.getTransactionId());
        }

        @Test
        void interactFoldsTheHitTowardTheEntity() {
            Vec3 entityPosition = new Vec3(511.5, 64.0, 0.0);
            ServerboundInteractPacket translated = (ServerboundInteractPacket) PacketTranslator.toServer(
                    new ServerboundInteractPacket(21, InteractionHand.MAIN_HAND, new Vec3(516.0, 64.5, 0.25), false),
                    context(entityId -> false, entityId -> entityId == 21 ? entityPosition : null));

            assertEquals(new Vec3(516.0, 64.5, 0.25), translated.location());
        }

        @Test
        void interactWithoutTheEntityFallsBackToThePlainWrap() {
            ServerboundInteractPacket translated = (ServerboundInteractPacket) PacketTranslator.toServer(
                    new ServerboundInteractPacket(21, InteractionHand.MAIN_HAND, new Vec3(516.0, 64.5, 0.25), false),
                    context());

            assertEquals(new Vec3(-508.0, 64.5, 0.25), translated.location());
        }
    }
}
