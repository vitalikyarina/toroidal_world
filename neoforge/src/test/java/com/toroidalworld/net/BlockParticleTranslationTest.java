package com.toroidalworld.net;

import static com.toroidalworld.net.PacketTranslatorFixture.CLIENT_BLOCK;
import static com.toroidalworld.net.PacketTranslatorFixture.CLIENT_X;
import static com.toroidalworld.net.PacketTranslatorFixture.CLIENT_Z;
import static com.toroidalworld.net.PacketTranslatorFixture.SERVER_BLOCK;
import static com.toroidalworld.net.PacketTranslatorFixture.SERVER_X;
import static com.toroidalworld.net.PacketTranslatorFixture.SERVER_Z;
import static com.toroidalworld.net.PacketTranslatorFixture.productionContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

class BlockParticleTranslationTest {
    @Test
    void blockParticlePositionMovesToTheHeldCopy() {
        BlockState state = Blocks.STONE.defaultBlockState();
        ClientboundLevelParticlesPacket translated = (ClientboundLevelParticlesPacket) PacketTranslator.toClient(
                new ClientboundLevelParticlesPacket(
                        new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(SERVER_BLOCK), false,
                        SERVER_X, 64.0, SERVER_Z, 0.0F, 0.0F, 0.0F, 0.0F, 1),
                productionContext());

        BlockParticleOption block = (BlockParticleOption) translated.getParticle();
        assertEquals(CLIENT_BLOCK, block.getPos());
        assertSame(state, block.getState());
    }

    @Test
    void explosionParticlesFollowTheTranslatedCentre() {
        BlockState state = Blocks.STONE.defaultBlockState();
        ClientboundExplodePacket translated = (ClientboundExplodePacket) PacketTranslator.toClient(
                new ClientboundExplodePacket(
                        SERVER_X, 70.0, SERVER_Z, 3.0F, List.of(), new Vec3(0.1, 0.2, 0.3),
                        Explosion.BlockInteraction.DESTROY,
                        new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(SERVER_BLOCK),
                        new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(SERVER_BLOCK),
                        SoundEvents.GENERIC_EXPLODE),
                productionContext());

        assertEquals(CLIENT_X, translated.getX());
        assertEquals(CLIENT_Z, translated.getZ());
        assertEquals(CLIENT_BLOCK, ((BlockParticleOption) translated.getSmallExplosionParticles()).getPos());
        assertEquals(CLIENT_BLOCK, ((BlockParticleOption) translated.getLargeExplosionParticles()).getPos());
    }
}
