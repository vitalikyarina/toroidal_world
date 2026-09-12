package com.toroidalworld.engine.noise;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.engine.DoubleStack;
import com.toroidalworld.engine.ObjectStack;

public final class GenerationTransformerContext {
    public static final double UNDECLARED_VERTICAL_SHARE = -1.0;

    public static double verticalShare(double xzScale, double yScale) {
        return xzScale == 0.0 ? UNDECLARED_VERTICAL_SHARE : yScale / xzScale;
    }

    public static final class Context {
        private WorldFold transformer = WorldFolds.NOOP;
        private @Nullable WorldFold routerBuild;
        private double horizontalScale = NoiseConstants.UNSCALED;
        private double verticalShare = UNDECLARED_VERTICAL_SHARE;
        private double xDivisor = NoiseConstants.UNDIVIDED;
        private double zDivisor = NoiseConstants.UNDIVIDED;
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

        public double verticalShare() {
            return this.verticalShare;
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

        public ScaleScope withScale(double scale, double verticalShare) {
            this.scaleScope.push();
            this.horizontalScale = scale;
            this.verticalShare = verticalShare;
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

        public BindingScope bind(WorldFold boundTransformer, SlotAxes boundAxes, double boundScale,
                double boundVerticalShare) {
            this.bindingScope.push();
            this.transformer = boundTransformer;
            this.slotAxes = boundAxes;
            this.horizontalScale = boundScale;
            this.verticalShare = boundVerticalShare;
            return this.bindingScope;
        }

        public final class BindingScope implements AutoCloseable {
            private final ObjectStack<WorldFold> previousTransformers = new ObjectStack<>();
            private final ObjectStack<SlotAxes> previousAxes = new ObjectStack<>();
            private final DoubleStack previousScales = new DoubleStack();
            private final DoubleStack previousShares = new DoubleStack();

            private BindingScope() {
            }

            private void push() {
                this.previousTransformers.push(transformer);
                this.previousAxes.push(slotAxes);
                this.previousScales.push(horizontalScale);
                this.previousShares.push(verticalShare);
            }

            @Override
            public void close() {
                transformer = this.previousTransformers.pop();
                slotAxes = this.previousAxes.pop();
                horizontalScale = this.previousScales.pop();
                verticalShare = this.previousShares.pop();
            }
        }

        public final class ScaleScope implements AutoCloseable {
            private final DoubleStack previousScales = new DoubleStack();
            private final DoubleStack previousShares = new DoubleStack();

            private ScaleScope() {
            }

            private void push() {
                this.previousScales.push(horizontalScale);
                this.previousShares.push(verticalShare);
            }

            public void rescale(double scale) {
                if (this.previousScales.isEmpty()) {
                    throw new IllegalStateException("rescale with no scale scope open");
                }

                horizontalScale = scale;
            }

            @Override
            public void close() {
                horizontalScale = this.previousScales.pop();
                verticalShare = this.previousShares.pop();
            }
        }

        public final class DivisorScope implements AutoCloseable {
            private final DoubleStack previousXDivisors = new DoubleStack();
            private final DoubleStack previousZDivisors = new DoubleStack();

            private DivisorScope() {
            }

            private void push() {
                this.previousXDivisors.push(xDivisor);
                this.previousZDivisors.push(zDivisor);
            }

            @Override
            public void close() {
                xDivisor = this.previousXDivisors.pop();
                zDivisor = this.previousZDivisors.pop();
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
