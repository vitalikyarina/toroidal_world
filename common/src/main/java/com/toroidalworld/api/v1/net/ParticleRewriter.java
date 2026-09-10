package com.toroidalworld.api.v1.net;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

/**
 * Rewrites one particle type of a mod's own on its way to a client. A particle whose options carry a world position —
 * a source, a destination, a block it came off — points a world away once the client holds a different copy, and
 * nothing but the mod that declared those options knows where inside them a position sits.
 *
 * <p>{@code clientOrigin} is where the particle itself lands in the client's frame; seat a position the particle
 * points at near it rather than near the player, or a long trail flips across the seam halfway along.</p>
 */
@FunctionalInterface
public interface ParticleRewriter<P extends ParticleOptions> {

    /** The options to send, or {@code particle} itself where nothing in it moved. */
    ParticleOptions rewrite(P particle, SeamContext context, Vec3 clientOrigin);
}
