package com.toroidalworld.noise;

import java.util.Arrays;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;

public final class GenerationTransformerContext {
    private static final double UNSCALED = 1.0;

    public static final class Context {
        private WorldLoopTransformer transformer = WorldLoopTransformer.NOOP;
        private double horizontalScale = UNSCALED;
        private final ScaleScope scaleScope = new ScaleScope();

        public WorldLoopTransformer transformer() {
            return this.transformer;
        }

        public @Nullable WorldLoopTransformer wrappedTransformer() {
            return this.transformer.isWrapped() ? this.transformer : null;
        }

        public double horizontalScale() {
            return this.horizontalScale;
        }

        public ScaleScope withScale(double scale) {
            this.scaleScope.push();
            this.horizontalScale = scale;
            return this.scaleScope;
        }

        public ScaleScope openScale() {
            this.scaleScope.push();
            return this.scaleScope;
        }

        public final class ScaleScope implements AutoCloseable {
            private double[] previousScales = new double[8];
            private int depth;

            private ScaleScope() {
            }

            private void push() {
                if (this.depth == this.previousScales.length) {
                    this.previousScales = Arrays.copyOf(this.previousScales, this.depth * 2);
                }

                this.previousScales[this.depth++] = horizontalScale;
            }

            public void rescale(double scale) {
                if (this.depth == 0) {
                    throw new IllegalStateException("rescale with no scale scope open");
                }

                horizontalScale = scale;
            }

            @Override
            public void close() {
                horizontalScale = this.previousScales[--this.depth];
            }
        }
    }

    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);

    public static Context context() {
        return CONTEXT.get();
    }

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

    public static void runWithTransformer(WorldLoopTransformer transformer, Runnable action) {
        withTransformer(transformer, () -> {
            action.run();
            return null;
        });
    }

    private GenerationTransformerContext() {
    }
}
