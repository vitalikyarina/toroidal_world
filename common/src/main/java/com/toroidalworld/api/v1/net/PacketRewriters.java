package com.toroidalworld.api.v1.net;

import java.util.function.BiFunction;

import com.toroidalworld.engine.net.PacketTranslator;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Where a mod says how its own packets cross the seam. Every vanilla packet carrying a world position is already
 * rewritten between the server's frame and the client's; a payload or a particle type this mod never heard of is
 * not, because nothing but its author knows which of its fields is a position.
 *
 * <p>Register from the mod's initialiser. The table closes at {@code MinecraftServer.runServer}, before the levels
 * load, and a later registration throws rather than being silently half-effective. A registered rewriter runs for
 * every payload of that exact class, on a folding level and nowhere else.</p>
 *
 * <p>A rewriter is handed a {@link SeamContext} for the connection the packet is on, and returns the value to
 * send — or the argument itself where nothing moved, which costs nothing.</p>
 */
public final class PacketRewriters {

    /**
     * How a payload of this mod's own is rewritten on its way to a client. Move every world position it carries
     * with {@link SeamContext#toClient}, or the client draws it a world away from where it belongs.
     */
    public static <P extends CustomPacketPayload> void registerClientboundPayload(Class<P> payloadType,
            BiFunction<P, SeamContext, CustomPacketPayload> rewriter) {
        PacketTranslator.registerClientboundPayloadRewriter(payloadType, rewriter::apply);
    }

    /**
     * How a payload of this mod's own is folded back on its way in from a client. A position the client sent is in
     * whatever copy that client holds; {@link SeamContext#toServer} is what makes it the position the server means.
     */
    public static <P extends CustomPacketPayload> void registerServerboundPayload(Class<P> payloadType,
            BiFunction<P, SeamContext, CustomPacketPayload> rewriter) {
        PacketTranslator.registerServerboundPayloadRewriter(payloadType, rewriter::apply);
    }

    /** How a particle type of this mod's own is rewritten on its way to a client. */
    public static <P extends ParticleOptions> void registerParticle(Class<P> particleType,
            ParticleRewriter<P> rewriter) {
        PacketTranslator.registerParticleRewriter(particleType,
                (particle, context, clientOrigin) -> rewriter.rewrite(particle, context, clientOrigin));
    }

    private PacketRewriters() {
    }
}
