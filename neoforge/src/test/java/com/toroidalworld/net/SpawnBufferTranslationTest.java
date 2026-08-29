package com.toroidalworld.net;

import static com.toroidalworld.net.PacketTranslatorFixture.CLIENT_BLOCK;
import static com.toroidalworld.net.PacketTranslatorFixture.CLIENT_X;
import static com.toroidalworld.net.PacketTranslatorFixture.CLIENT_Z;
import static com.toroidalworld.net.PacketTranslatorFixture.SERVER_BLOCK;
import static com.toroidalworld.net.PacketTranslatorFixture.SERVER_X;
import static com.toroidalworld.net.PacketTranslatorFixture.SERVER_Z;
import static com.toroidalworld.net.PacketTranslatorFixture.context;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.payload.AdvancedAddEntityPayload;

class SpawnBufferTranslationTest {
    private static final int ENTITY_ID = 21;
    private static final int MISSING_ENTITY_ID = 22;

    private static final String POS_KEY = "Pos";
    private static final String OFFSET_KEY = "From";
    private static final String TILE_X_KEY = "TileX";
    private static final String TILE_Y_KEY = "TileY";
    private static final String TILE_Z_KEY = "TileZ";

    private static final double POS_Y = 64.0;
    private static final byte TAIL_BYTE = 7;

    private static final Class<?> REGISTERED_TYPE = ArmorStand.class;
    private static final Class<?> BLOCK_ATTACHED_TYPE = Painting.class;
    private static final Class<?> UNREGISTERED_TYPE = Boat.class;

    static {
        SpawnBufferFold.register(REGISTERED_TYPE, TagPositions.PositionShape.VEC3_LIST, POS_KEY);
        SpawnBufferTranslation.register();
    }

    private static TranslationContext contextHolding(Class<?> entityClass) {
        return context(entityId -> false, entityId -> null,
                entityId -> entityId == ENTITY_ID ? entityClass : null);
    }

    private static ListTag doubleList(double x, double y, double z) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(x));
        list.add(DoubleTag.valueOf(y));
        list.add(DoubleTag.valueOf(z));
        return list;
    }

    private static CompoundTag spawnData(double x, double z) {
        CompoundTag tag = new CompoundTag();
        tag.put(POS_KEY, doubleList(x, POS_Y, z));
        tag.put(OFFSET_KEY, doubleList(-1.0, 0.0, -1.0));
        return tag;
    }

    private static CompoundTag attachmentData(BlockPos attachment) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TILE_X_KEY, attachment.getX());
        tag.putInt(TILE_Y_KEY, attachment.getY());
        tag.putInt(TILE_Z_KEY, attachment.getZ());
        return tag;
    }

    private static ClientboundCustomPayloadPacket packetOf(int entityId, CompoundTag tag, boolean withTail) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeNbt(tag);
        if (withTail) {
            buffer.writeByte(TAIL_BYTE);
        }

        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return new ClientboundCustomPayloadPacket(new AdvancedAddEntityPayload(entityId, bytes));
    }

    private static FriendlyByteBuf bufferOf(Packet<?> packet) {
        AdvancedAddEntityPayload payload =
                (AdvancedAddEntityPayload) ((ClientboundCustomPayloadPacket) packet).payload();
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.customPayload()));
    }

    private static Vec3 vec3In(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
        return new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
    }

    @Test
    void aSpawnPositionAWorldAwayReachesTheViewerOnItsOwnCopy() {
        Packet<?> translated = PacketTranslator.toClient(
                packetOf(ENTITY_ID, spawnData(SERVER_X, SERVER_Z), false), contextHolding(REGISTERED_TYPE));

        assertEquals(new Vec3(CLIENT_X, POS_Y, CLIENT_Z), vec3In(bufferOf(translated).readNbt(), POS_KEY));
    }

    @Test
    void anAttachmentBlockAWorldAwayReachesTheViewerOnItsOwnCopy() {
        Packet<?> translated = PacketTranslator.toClient(
                packetOf(ENTITY_ID, attachmentData(SERVER_BLOCK), false), contextHolding(BLOCK_ATTACHED_TYPE));

        CompoundTag seated = bufferOf(translated).readNbt();
        assertEquals(CLIENT_BLOCK, new BlockPos(seated.getInt(TILE_X_KEY), seated.getInt(TILE_Y_KEY),
                seated.getInt(TILE_Z_KEY)));
    }

    @Test
    void anAttachmentBlockAlreadyInTheViewersFrameIsThePacketBack() {
        ClientboundCustomPayloadPacket packet = packetOf(ENTITY_ID, attachmentData(CLIENT_BLOCK), false);

        assertSame(packet, PacketTranslator.toClient(packet, contextHolding(BLOCK_ATTACHED_TYPE)));
    }

    @Test
    void theEntityIdOpeningThePayloadIsCarriedThrough() {
        Packet<?> translated = PacketTranslator.toClient(
                packetOf(ENTITY_ID, spawnData(SERVER_X, SERVER_Z), false), contextHolding(REGISTERED_TYPE));

        AdvancedAddEntityPayload payload =
                (AdvancedAddEntityPayload) ((ClientboundCustomPayloadPacket) translated).payload();
        assertEquals(ENTITY_ID, payload.entityId());
    }

    @Test
    void theOffsetKeysBesideThePositionAreLeftAsTheyAre() {
        Packet<?> translated = PacketTranslator.toClient(
                packetOf(ENTITY_ID, spawnData(SERVER_X, SERVER_Z), false), contextHolding(REGISTERED_TYPE));

        assertEquals(new Vec3(-1.0, 0.0, -1.0), vec3In(bufferOf(translated).readNbt(), OFFSET_KEY));
    }

    @Test
    void whateverTheEntityWroteAfterTheCompoundRidesAlongUntouched() {
        Packet<?> translated = PacketTranslator.toClient(
                packetOf(ENTITY_ID, spawnData(SERVER_X, SERVER_Z), true), contextHolding(REGISTERED_TYPE));

        FriendlyByteBuf buffer = bufferOf(translated);
        buffer.readNbt();
        assertEquals(TAIL_BYTE, buffer.readByte());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void aBufferAlreadyInTheViewersFrameIsThePacketBack() {
        ClientboundCustomPayloadPacket packet = packetOf(ENTITY_ID, spawnData(CLIENT_X, CLIENT_Z), false);

        assertSame(packet, PacketTranslator.toClient(packet, contextHolding(REGISTERED_TYPE)));
    }

    @Test
    void anEntityTypeWithNoRegistrationIsThePacketBack() {
        ClientboundCustomPayloadPacket packet = packetOf(ENTITY_ID, spawnData(SERVER_X, SERVER_Z), false);

        assertSame(packet, PacketTranslator.toClient(packet, contextHolding(UNREGISTERED_TYPE)));
    }

    @Test
    void anEntityTheServerNoLongerHoldsIsThePacketBack() {
        ClientboundCustomPayloadPacket packet = packetOf(MISSING_ENTITY_ID, spawnData(SERVER_X, SERVER_Z), false);

        assertSame(packet, PacketTranslator.toClient(packet, contextHolding(REGISTERED_TYPE)));
    }
}
