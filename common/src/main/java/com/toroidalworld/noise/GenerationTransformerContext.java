package com.toroidalworld.noise;

import java.util.Arrays;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

public final class GenerationTransformerContext {
    private static final double UNSCALED = 1.0;
    private static final double UNDIVIDED = 1.0;

    public static final class Context {
        private WorldFold transformer = WorldFolds.NOOP;
        private @Nullable WorldFold routerBuild;
        private double horizontalScale = UNSCALED;
        private double xDivisor = UNDIVIDED;
        private double zDivisor = UNDIVIDED;
        private SlotAxes slotAxes = SlotAxes.DEFAULT;
        private final ScaleScope scaleScope = new ScaleScope();
        private final DivisorScope divisorScope = new DivisorScope();
        private final BindingScope bindingScope = new BindingScope();

        public WorldFold transformer() {
            return this.transformer;
        }

        public @Nullable WorldFold wrappedTransformer() {
            return this.transformer.isWrapped() ? this.transformer : null;
        }

        public @Nullable WorldFold routerBuildTransformer() {
            return this.routerBuild;
        }

        public double horizontalScale() {
            return this.horizontalScale;
        }

        public double xDivisor() {
            return this.xDivisor;
        }

        public double zDivisor() {
            return this.zDivisor;
        }

        public SlotAxes slotAxes() {
            return this.slotAxes;
        }

        public ScaleScope withScale(double scale) {
            this.scaleScope.push();
            this.horizontalScale = scale;
            return this.scaleScope;
        }

        public DivisorScope withDivisors(double xDivisor, double zDivisor) {
            this.divisorScope.push();
            this.xDivisor = xDivisor;
            this.zDivisor = zDivisor;
            return this.divisorScope;
        }

        public ScaleScope openScale() {
            this.scaleScope.push();
            return this.scaleScope;
        }

        public BindingScope bind(WorldFold boundTransformer, SlotAxes boundAxes, double boundScale) {
            this.bindingScope.push();
            this.transformer = boundTransformer;
            this.slotAxes = boundAxes;
            this.horizontalScale = boundScale;
            return this.bindingScope;
        }

        public final class BindingScope implements AutoCloseable {
            private WorldFold[] previousTransformers = new WorldFold[8];
            private SlotAxes[] previousAxes = new SlotAxes[8];
            private double[] previousScales = new double[8];
            private int depth;

            private BindingScope() {
            }

            private void push() {
                if (this.depth == this.previousTransformers.length) {
                    this.previousTransformers = Arrays.copyOf(this.previousTransformers, this.depth * 2);
                    this.previousAxes = Arrays.copyOf(this.previousAxes, this.depth * 2);
                    this.previousScales = Arrays.copyOf(this.previousScales, this.depth * 2);
                }

                this.previousTransformers[this.depth] = transformer;
                this.previousAxes[this.depth] = slotAxes;
                this.previousScales[this.depth] = horizontalScale;
                this.depth++;
            }

            @Override
            public void close() {
                this.depth--;
                transformer = this.previousTransformers[this.depth];
                slotAxes = this.previousAxes[this.depth];
                horizontalScale = this.previousScales[this.depth];
            }
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

        public final class DivisorScope implements AutoCloseable {
            private double[] previousXDivisors = new double[8];
            private double[] previousZDivisors = new double[8];
            private int depth;

            private DivisorScope() {
            }

            private void push() {
                if (this.depth == this.previousXDivisors.length) {
                    this.previousXDivisors = Arrays.copyOf(this.previousXDivisors, this.depth * 2);
                    this.previousZDivisors = Arrays.copyOf(this.previousZDivisors, this.depth * 2);
                }

                this.previousXDivisors[this.depth] = xDivisor;
                this.previousZDivisors[this.depth] = zDivisor;
                this.depth++;
            }

            @Override
            public void close() {
                this.depth--;
                xDivisor = this.previousXDivisors[this.depth];
                zDivisor = this.previousZDivisors[this.depth];
            }
        }
    }

    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);

    public static Context context() {
        return CONTEXT.get();
    }

    public static <T> T withTransformer(WorldFold transformer, Supplier<T> action) {
        Context context = CONTEXT.get();
        WorldFold previous = context.transformer;
        context.transformer = transformer;

        try {
            return action.get();
        } finally {
            context.transformer = previous;
        }
    }

    public static void runWithTransformer(WorldFold transformer, Runnable action) {
        withTransformer(transformer, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T withRouterBuild(@Nullable WorldFold transformer, Supplier<T> action) {
        Context context = CONTEXT.get();
        WorldFold previous = context.routerBuild;
        context.routerBuild = transformer;

        try {
            return action.get();
        } finally {
            context.routerBuild = previous;
        }
    }

    private GenerationTransformerContext() {
    }
}
