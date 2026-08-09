package com.toroidalworld.net;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;

// NeoForge's block particle carries an extra absolute position vanilla's does not — the block whose model data the
// client looks up. It takes the chunk-anchored fold: it has to land in the copy of the chunk the client holds, or the
// lookup resolves against nothing and falls back to the default sprite. Registered from the loader wiring because the
// extra position is NeoForge's own patch, invisible to the loader-free translator core.
public final class BlockParticleTranslation {
    public static void register() {
        PacketTranslator.registerParticleRewriter(BlockParticleOption.class, (particle, context, clientOrigin) -> {
            BlockPos serverPos = particle.getPos();
            if (serverPos == null) {
                return particle;
            }

            return new BlockParticleOption(particle.getType(), particle.getState())
                    .setPos(PacketTranslator.toClientBlock(context, serverPos));
        });
    }

    private BlockParticleTranslation() {
    }
}
