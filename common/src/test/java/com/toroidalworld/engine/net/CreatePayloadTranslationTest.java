package com.toroidalworld.engine.net;

import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.simibubi.create.content.contraptions.glue.GlueEffectPacket;
import com.simibubi.create.content.equipment.bell.SoulPulseEffectPacket;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryEffectPacket;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmPlacementPacket;
import com.simibubi.create.content.logistics.depot.EjectorPlacementPacket;
import com.simibubi.create.content.logistics.packagePort.PackagePortPlacementPacket;
import com.simibubi.create.content.logistics.packagerLink.WiFiEffectPacket;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterEffectPacket;
import com.simibubi.create.content.logistics.stockTicker.LogisticalStockResponsePacket;
import com.toroidalworld.compat.create.CreateTranslation;
import com.toroidalworld.engine.seam.ClientPosition;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;

class CreatePayloadTranslationTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final IntFunction<RegistryFriendlyByteBuf> BUFFERS =
            capacity -> new RegistryFriendlyByteBuf(Unpooled.buffer(capacity), REGISTRIES);

    private static final int VIEW_DISTANCE = 16;

    private static final double NEAR_THE_EAST_EDGE = 200.0;
    private static final double AT_THE_CENTRE = 8.0;
    private static final double PLAYER_Z = 8.0;

    private static final BlockPos SERVER_BLOCK = new BlockPos(-232, 64, 8);
    private static final BlockPos CLIENT_BLOCK = new BlockPos(280, 64, 8);
    private static final BlockPos INLAND_BLOCK = new BlockPos(200, 64, 8);

    private static final BlockPos SERVER_MIRROR = new BlockPos(232, 64, 8);
    private static final BlockPos SERVER_PLACED = new BlockPos(-232, 64, 8);
    private static final BlockPos PLACED_BESIDE_THE_MIRROR = new BlockPos(280, 64, 8);

    @BeforeAll
    static void registerTheCreateRewriters() {
        CreateTranslation.register();
    }

    private static TranslationContext contextAt(double playerX) {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(playerX, PLAYER_Z, Level.OVERWORLD, PER_AXIS);
        return new TranslationContext(PER_AXIS, mirror, BUFFERS, Level.OVERWORLD,
                VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityId -> null, entityId -> null, () -> {},
                PacketTranslator.production());
    }

    private static CustomPacketPayload seated(CustomPacketPayload payload, double playerX) {
        ClientboundCustomPayloadPacket sent = new ClientboundCustomPayloadPacket(payload);
        return ((ClientboundCustomPayloadPacket) PacketTranslator.toClient(sent, contextAt(playerX))).payload();
    }

    private static CustomPacketPayload seated(CustomPacketPayload payload) {
        return seated(payload, NEAR_THE_EAST_EDGE);
    }

    @Test
    void everyPlacementEchoLandsOnTheCopyTheClientHolds() {
        assertEquals(new ArmPlacementPacket.ClientBoundRequest(CLIENT_BLOCK),
                seated(new ArmPlacementPacket.ClientBoundRequest(SERVER_BLOCK)));
        assertEquals(new EjectorPlacementPacket.ClientBoundRequest(CLIENT_BLOCK),
                seated(new EjectorPlacementPacket.ClientBoundRequest(SERVER_BLOCK)));
        assertEquals(new PackagePortPlacementPacket.ClientBoundRequest(CLIENT_BLOCK),
                seated(new PackagePortPlacementPacket.ClientBoundRequest(SERVER_BLOCK)));
    }

    @Test
    void everySingleBlockEffectLandsOnTheCopyTheClientHolds() {
        assertEquals(new SoulPulseEffectPacket(CLIENT_BLOCK, 5, true),
                seated(new SoulPulseEffectPacket(SERVER_BLOCK, 5, true)));
        assertEquals(new WiFiEffectPacket(CLIENT_BLOCK), seated(new WiFiEffectPacket(SERVER_BLOCK)));
        assertEquals(new GlueEffectPacket(CLIENT_BLOCK, Direction.UP, true),
                seated(new GlueEffectPacket(SERVER_BLOCK, Direction.UP, true)));
        assertEquals(new RedstoneRequesterEffectPacket(CLIENT_BLOCK, true),
                seated(new RedstoneRequesterEffectPacket(SERVER_BLOCK, true)));
        assertEquals(new LogisticalStockResponsePacket(true, CLIENT_BLOCK, List.of()),
                seated(new LogisticalStockResponsePacket(true, SERVER_BLOCK, List.of())));
    }

    @Test
    void theSymmetryMirrorAndItsPlacementsLandInOneFrame() {
        assertEquals(new SymmetryEffectPacket(CLIENT_BLOCK, List.of(CLIENT_BLOCK)),
                seated(new SymmetryEffectPacket(SERVER_BLOCK, List.of(SERVER_BLOCK))));
    }

    @Test
    void aSymmetryPlacementPastTheMirrorSeatsOnTheMirrorRatherThanOnThePlayer() {
        assertEquals(new SymmetryEffectPacket(SERVER_MIRROR, List.of(PLACED_BESIDE_THE_MIRROR)),
                seated(new SymmetryEffectPacket(SERVER_MIRROR, List.of(SERVER_PLACED)), AT_THE_CENTRE));
    }

    @Test
    void aPayloadInThePlayersOwnFrameTravelsUntouched() {
        WiFiEffectPacket inland = new WiFiEffectPacket(INLAND_BLOCK);
        assertSame(inland, seated(inland));

        SymmetryEffectPacket inlandSymmetry = new SymmetryEffectPacket(INLAND_BLOCK, List.of(INLAND_BLOCK));
        assertSame(inlandSymmetry, seated(inlandSymmetry));
    }
}
