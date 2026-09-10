package com.toroidalworld.api.v1.gen;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.core.GenerationMoments;

import net.minecraft.world.level.levelgen.RandomState;

/**
 * The moments world generation offers a feature, and the door a mod hooks them at. A hook registers at startup,
 * before the registration boundary closes at {@code MinecraftServer.runServer}, and runs once per {@link RandomState}
 * built for a folding level.
 */
public final class GenerationHooks {

    /**
     * A hook run as a folding level's {@link RandomState} finishes building — the moment its noise router exists and
     * nothing has sampled it yet, so rewriting a noise in that router still reaches every chunk. The shape and the
     * options are the ones the world was created with.
     *
     * <p><strong>A hook gates itself.</strong> Every registered hook runs for every folding level of every world,
     * whatever shape made it and whichever mod declared that shape — a hook is not scoped to the shape it was
     * registered beside, and a level is one of several a world builds, so it runs once per dimension too. Read
     * {@code shape} and {@code options} and return early unless both are what this hook is for; reaching for a span
     * on an axis that does not loop throws.</p>
     */
    @FunctionalInterface
    public interface RandomStateHook {
        void run(RandomState randomState, ToroidalShape shape, GenerationOptions options, int seaLevel);
    }

    /**
     * Registers {@code hook} at the {@link RandomState} moment under {@code key}, which orders the hooks against one
     * another and must be unique across every mod — prefix it with your mod id where a clash is possible.
     *
     * @throws IllegalStateException if the registration boundary has already closed
     */
    public static void atRandomState(String key, RandomStateHook hook) {
        GenerationMoments.atRandomState(key, hook);
    }

    private GenerationHooks() {
    }
}
