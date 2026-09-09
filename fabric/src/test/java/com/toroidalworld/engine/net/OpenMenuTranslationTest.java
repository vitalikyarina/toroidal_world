package com.toroidalworld.engine.net;

import static com.toroidalworld.engine.net.PacketTranslatorFixture.CLIENT_BLOCK;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.SERVER_BLOCK;
import static com.toroidalworld.engine.net.PacketTranslatorFixture.context;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.ToroidalWorld;

import net.fabricmc.fabric.impl.screenhandler.Networking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;

class OpenMenuTranslationTest {
    private static final Identifier MENU_ID = Identifier.fromNamespaceAndPath(ToroidalWorld.MODID, "probe_menu");

    private static final int CONTAINER_ID = 3;

    private static final Component TITLE = Component.literal("Probe");

    private static final String LABEL = "a label the fold knows nothing about";

    private static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> POSITION_CODEC = BlockPos.STREAM_CODEC.cast();

    private static final StreamCodec<RegistryFriendlyByteBuf, String> LABEL_CODEC = ByteBufCodecs.STRING_UTF8.cast();

    @BeforeAll
    static void registerTheRewriterThroughItsGate() {
        OpenMenuTranslation.register();
    }

    private static <D> ClientboundCustomPayloadPacket openScreen(StreamCodec<RegistryFriendlyByteBuf, D> codec, D data) {
        return new ClientboundCustomPayloadPacket(
                new Networking.OpenScreenPayload<>(MENU_ID, CONTAINER_ID, TITLE, codec, data));
    }

    @Test
    void openingPositionMovesToTheClientFrame() {
        ClientboundCustomPayloadPacket translated =
                (ClientboundCustomPayloadPacket) PacketTranslator.toClient(
                        openScreen(POSITION_CODEC, SERVER_BLOCK), context());

        Networking.OpenScreenPayload<?> open = (Networking.OpenScreenPayload<?>) translated.payload();
        assertEquals(CLIENT_BLOCK, open.data());
        assertEquals(MENU_ID, open.identifier());
        assertEquals(CONTAINER_ID, open.syncId());
        assertSame(TITLE, open.title());
        assertSame(POSITION_CODEC, open.innerCodec());
    }

    @Test
    void openingPositionAlreadyInTheClientFrameKeepsThePacket() {
        ClientboundCustomPayloadPacket packet = openScreen(POSITION_CODEC, CLIENT_BLOCK);

        assertSame(packet, PacketTranslator.toClient(packet, context()));
    }

    @Test
    void openingDataThatIsNotAPositionPassesThrough() {
        ClientboundCustomPayloadPacket packet = openScreen(LABEL_CODEC, LABEL);

        assertSame(packet, PacketTranslator.toClient(packet, context()));
    }
}
