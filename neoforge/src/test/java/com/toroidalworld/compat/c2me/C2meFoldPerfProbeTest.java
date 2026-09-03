package com.toroidalworld.compat.c2me;

import static com.toroidalworld.noise.DensityFunctionFixture.CLIMATE_XZ_SCALE;
import static com.toroidalworld.noise.DensityFunctionFixture.SQUARE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.DoubleSupplier;

import org.junit.jupiter.api.Test;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.SlotAxes;

import net.minecraft.world.level.levelgen.DensityFunction;

class C2meFoldPerfProbeTest {
    private static final int SAMPLES = 1 << 20;
    private static final int WARMUP_REPS = 3;
    private static final int TIMED_REPS = 5;

    private static final int CONTEXT_POINTS = 512;

    private static final int SLICE_FILL = 49;
    private static final int CELL_FILL = 128;

    private static final double SHIFTED_NOISE_2D_VERTICAL_SHARE = 0.0;

    @Test
    void measuresTheFoldOnTheCompiledPath() {
        DensityFunction source = C2meCompiledFunctions.climateSource();
        DensityFunction folded = C2meCompiledFunctions.compileFolded("toroidal_fold_probe_folded", source, SQUARE);
        DensityFunction unfolded = C2meCompiledFunctions.compileUnfolded("toroidal_fold_probe_unfolded", source);
        assertTheFoldChangesTheValues(folded, unfolded);

        StringBuilder report = new StringBuilder();
        report.append("C2ME fold perf probe — ").append(SAMPLES).append(" samples/rep, best of ").append(TIMED_REPS)
                .append(" reps after ").append(WARMUP_REPS)
                .append(" warmups, 32-chunk (512-block) world.").append(System.lineSeparator())
                .append("The control is the same source compiled unfolded, which is what an unwrapped level emits;")
                .append(" both rows of a pair carry C2ME's on-demand coordinate materialization, so a row's own")
                .append(" value is an upper bound and only the difference within a pair prices the fold.")
                .append(System.lineSeparator());

        C2meCompiledFunctions.Points points = C2meCompiledFunctions.Points.over(SQUARE, CONTEXT_POINTS);
        DensityFunction.FunctionContext[] contexts = new DensityFunction.FunctionContext[CONTEXT_POINTS];
        for (int i = 0; i < CONTEXT_POINTS; i++) {
            contexts[i] = points.forIndex(i);
        }

        report.append(measureCompute("compute, folded", folded, contexts));
        report.append(measureCompute("compute, unfolded (control)", unfolded, contexts));
        report.append(measureFill("fillArray, slice column, folded", folded, SLICE_FILL));
        report.append(measureFill("fillArray, slice column, unfolded (control)", unfolded, SLICE_FILL));
        report.append(measureFill("fillArray, cell cache, folded", folded, CELL_FILL));
        report.append(measureFill("fillArray, cell cache, unfolded (control)", unfolded, CELL_FILL));
        report.append(measureContextLookup());
        report.append(measureBinding());

        Path out = Path.of("build", "reports", "c2me-fold-perf-probe.txt");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, report.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void assertTheFoldChangesTheValues(DensityFunction folded, DensityFunction unfolded) {
        C2meCompiledFunctions.Points points = C2meCompiledFunctions.Points.over(SQUARE, CONTEXT_POINTS);
        double[] foldedValues = new double[CONTEXT_POINTS];
        double[] unfoldedValues = new double[CONTEXT_POINTS];

        folded.fillArray(foldedValues, points);
        unfolded.fillArray(unfoldedValues, points);

        assertFalse(Arrays.equals(foldedValues, unfoldedValues),
                "the folded and unfolded compiles agree everywhere, so this probe times one path twice");
    }

    private String measureCompute(String label, DensityFunction function,
            DensityFunction.FunctionContext[] contexts) {
        return measure(label, SAMPLES, () -> {
            double sink = 0.0;
            for (int i = 0; i < SAMPLES; i++) {
                sink += function.compute(contexts[i & (CONTEXT_POINTS - 1)]);
            }

            return sink;
        });
    }

    private String measureFill(String label, DensityFunction function, int length) {
        C2meCompiledFunctions.Points points = C2meCompiledFunctions.Points.over(SQUARE, length);
        double[] values = new double[length];
        int fills = SAMPLES / length;

        return measure(label + " (" + length + " points)", fills * length, () -> {
            double sink = 0.0;
            for (int fill = 0; fill < fills; fill++) {
                function.fillArray(values, points);
                sink += values[0];
            }

            return sink;
        });
    }

    private String measureContextLookup() {
        return measure("thread-local context lookup", SAMPLES, () -> {
            double sink = 0.0;
            for (int i = 0; i < SAMPLES; i++) {
                sink += GenerationTransformerContext.context().horizontalScale();
            }

            return sink;
        });
    }

    private String measureBinding() {
        return measure("thread-local lookup plus bind and close", SAMPLES, () -> {
            double sink = 0.0;
            for (int i = 0; i < SAMPLES; i++) {
                Context context = GenerationTransformerContext.context();
                try (Context.BindingScope _ = context.bind(SQUARE, SlotAxes.DEFAULT, CLIMATE_XZ_SCALE,
                        SHIFTED_NOISE_2D_VERTICAL_SHARE)) {
                    sink += context.horizontalScale();
                }
            }

            return sink;
        });
    }

    private String measure(String label, int samples, DoubleSupplier body) {
        double sink = 0.0;
        long best = Long.MAX_VALUE;
        for (int rep = 0; rep < WARMUP_REPS + TIMED_REPS; rep++) {
            long start = System.nanoTime();
            sink += body.getAsDouble();
            long elapsed = System.nanoTime() - start;
            if (rep >= WARMUP_REPS && elapsed < best) {
                best = elapsed;
            }
        }

        assertTrue(Double.isFinite(sink));
        return String.format(Locale.ROOT, "  %-50s %8.2f ns/sample%n", label, (double) best / samples);
    }
}
