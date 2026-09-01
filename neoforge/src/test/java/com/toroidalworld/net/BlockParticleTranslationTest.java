package com.toroidalworld.net;

import static com.toroidalworld.net.PacketTranslatorFixture.CLIENT_BLOCK;
import static com.toroidalworld.net.PacketTranslatorFixture.SERVER_BLOCK;
import static com.toroidalworld.net.PacketTranslatorFixture.SERVER_X;
import static com.toroidalworld.net.PacketTranslatorFixture.SERVER_Z;
import static com.toroidalworld.net.PacketTranslatorFixture.context;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

class BlockParticleTranslationTest {
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
}
