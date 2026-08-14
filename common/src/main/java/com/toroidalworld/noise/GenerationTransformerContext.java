package com.toroidalworld.noise;

import java.util.Arrays;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;

// Vanilla noise samplers are shared, long-lived objects with no access to the level they are generating for.
// The worldgen step parks its transformer here so the noise mixins can pick it up, and each caller parks the horizontal
// scale it would have applied to X/Z — the wrapped noise turns that scale into the lattice period it wraps by,
// which is what keeps vanilla frequencies alive across the seam.
//
// Both are thread-local, and the transformer is thread-local *again* after a detour worth knowing about. It was made a
// plain static because two of the eight chunk steps hand their sampling to a background executor (fillFromNoise and
// createBiomes are supplyAsync), so a value bound at the step ran on one thread and was read on another — as a
// ThreadLocal it was simply absent there, and roughly half of all noise calls silently took the vanilla branch.
//
// A shared static bridged those threads, at the price of one global for the whole game: with two wrapped dimensions,
// one dimension's width could be read while the other's chunk was mid-sample, and the resulting terrain — which does
// not tile at the seam — was persisted. The fix is to bind where the work actually runs rather than where it is
// queued, so nothing has to cross a thread at all: the two async paths bind inside doFill / doCreateBiomes (see
// NoiseBasedChunkGeneratorMixin), and the six synchronous steps keep binding at the step itself.
public final class GenerationTransformerContext {
    private static final double UNSCALED = 1.0;

    // One holder per thread carrying everything a sample needs together — the transformer and the scale — and there
    // is no accessor path that fetches either on its own. A ThreadLocal lookup costs more than reading a field:
    // fetching twice per sample measured ~9% slower over a chunk's noise fill than the shared static this replaced.
    // Keeping a current()-style shortcut beside this would put a slow path in the code that reads exactly like the
    // fast one.
    //
    // Mutable fields rather than a record for the same reason the scale was a holder before: it is written once per
    // sample, and a fresh object (or a boxed Double) per write allocates on the hottest path in the mod.
    // No call site has declared its vertical-to-horizontal scale ratio — the octave variance correction stays off.
    public static final double UNDECLARED_VERTICAL_SHARE = -1.0;

    public static final class Context {
        private WorldLoopTransformer transformer = WorldLoopTransformer.NOOP;
        private double horizontalScale = UNSCALED;
        private double verticalShare = UNDECLARED_VERTICAL_SHARE;
        private final ScaleScope scaleScope = new ScaleScope();

        public WorldLoopTransformer transformer() {
            return this.transformer;
        }

        // The worldgen-thread twin of WorldLoopAttachments.wrappedTransformerOf: the transformer only when this
        // thread is bound to a wrapping generation, else null — so a sampler can fetch and bail in one step. It reads
        // off the holder rather than sitting beside context() as a static, because a static would repeat the
        // ThreadLocal lookup the holder exists to spare.
        public @Nullable WorldLoopTransformer wrappedTransformer() {
            return this.transformer.isWrapped() ? this.transformer : null;
        }

        // Callers follow a stack discipline the API now enforces: the only way to change the scale is to open a scope,
        // and closing it restores what was there — however the block ends, and whatever rescale set in between.
        public double horizontalScale() {
            return this.horizontalScale;
        }

        // The sampled field's vertical-to-horizontal scale ratio (0 = vertically flat, UNDECLARED_VERTICAL_SHARE =
        // unknown, correction off). Set at the density-function call sites that know both scales; inner scopes (the
        // detune layer, the octave walk) inherit it, so it describes the field, not the innermost scale push.
        public double verticalShare() {
            return this.verticalShare;
        }

        // The scale twin of withTransformer, minus the lambda: a noise sample is the hottest path in the mod, and a
        // capturing closure per sample would allocate where even a boxed Double was too much. One reusable scope per
        // context carries the previous values; try-with-resources guarantees the restore.
        public ScaleScope withScale(double scale) {
            this.scaleScope.push();
            this.horizontalScale = scale;
            return this.scaleScope;
        }

        public ScaleScope withScale(double scale, double verticalShare) {
            this.scaleScope.push();
            this.horizontalScale = scale;
            this.verticalShare = verticalShare;
            return this.scaleScope;
        }

        // A scope opened at the current value, for the octave loops that only rescale as they walk.
        public ScaleScope openScale() {
            this.scaleScope.push();
            return this.scaleScope;
        }

        public final class ScaleScope implements AutoCloseable {
            private double[] previousScales = new double[8];
            private double[] previousShares = new double[8];
            private int depth;

            private ScaleScope() {
            }

            private void push() {
                if (this.depth == this.previousScales.length) {
                    this.previousScales = Arrays.copyOf(this.previousScales, this.depth * 2);
                    this.previousShares = Arrays.copyOf(this.previousShares, this.depth * 2);
                }

                this.previousScales[this.depth] = horizontalScale;
                this.previousShares[this.depth++] = verticalShare;
            }

            // A write bounded by the innermost open scope — close() restores the entry value regardless of what was
            // set here. Refusing to write with no scope open is the point: an unbounded write is exactly the
            // pooled-thread leak this API exists to prevent.
            public void rescale(double scale) {
                if (this.depth == 0) {
                    throw new IllegalStateException("rescale with no scale scope open");
                }

                horizontalScale = scale;
            }

            @Override
            public void close() {
                horizontalScale = this.previousScales[--this.depth];
                verticalShare = this.previousShares[this.depth];
            }
        }
    }

    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);

    // Fetch once per sample and read everything off it.
    public static Context context() {
        return CONTEXT.get();
    }

    // The only way to bind: scoped, restoring the previous transformer however the call ends. There is deliberately no
    // fire-and-forget setter — the chunk steps, the async noise fills and the out-of-step entry points (spawn selection,
    // height queries) all run on shared pools, and a value left on a pooled thread is read by whatever lands there next
    // with no binder of its own. A leftover like that once turned an ordinary world's terrain periodic.
    public static <T> T withTransformer(WorldLoopTransformer transformer, Supplier<T> action) {
        Context context = CONTEXT.get();
        WorldLoopTransformer previous = context.transformer;
        context.transformer = transformer;

        try {
            return action.get();
        } finally {
            context.transformer = previous;
        }
    }

    // Named apart from the supplier form rather than overloaded: a lambda body that happens to end in a value would
    // otherwise pick the wrong one silently.
    public static void runWithTransformer(WorldLoopTransformer transformer, Runnable action) {
        withTransformer(transformer, () -> {
            action.run();
            return null;
        });
    }

    private GenerationTransformerContext() {
    }
}
