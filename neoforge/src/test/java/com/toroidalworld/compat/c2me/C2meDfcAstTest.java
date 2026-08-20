package com.toroidalworld.compat.c2me;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.noise.ContextScaledNoise;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.NoiseRouterBuild;
import com.toroidalworld.options.WorldLoopBounds;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class C2meDfcAstTest {
    private static final long SEED = 0x0153EL;

    private static final WorldLoopTransformer WRAPPED =
            new WorldLoopTransformer(new WorldLoopBounds(-16, 16, -16, 16));

    private static final WorldLoopTransformer RECTANGULAR =
            new WorldLoopTransformer(new WorldLoopBounds(-16, 16, -8, 8));

    private static final List<WorldLoopTransformer> WORLDS = List.of(WRAPPED, RECTANGULAR);

    private static final int PERIODICITY_SAMPLES = 32;
    private static final int WORLD_HEIGHT = 384;
    private static final int LOWEST_Y = -64;

    private static final int SAMPLE_X = 137;
    private static final int SAMPLE_Y = 61;
    private static final int SAMPLE_Z = -211;

    private static final double XZ_SCALE = 0.7;
    private static final double Y_SCALE = 0.3;
    private static final double SHIFT_X = 3.0;
    private static final double SHIFT_Y = 7.0;
    private static final double SHIFT_Z = -5.0;

    private static final NormalNoise.NoiseParameters PARAMETERS =
            new NormalNoise.NoiseParameters(-6, DoubleArrayList.of(1.0, 1.0, 1.0));

    private static final Holder<NormalNoise.NoiseParameters> NOISE_DATA = Holder.direct(PARAMETERS);

    private static final DensityFunction.NoiseHolder NOISE = new DensityFunction.NoiseHolder(
            NOISE_DATA, NormalNoise.create(new LegacyRandomSource(SEED), PARAMETERS));

    @Test
    void noiseFoldsToTheSameSample() {
        assertFoldMatchesVanilla(withLiveNoise(DensityFunctions.noise(NOISE_DATA, XZ_SCALE, Y_SCALE)));
    }

    @Test
    void shiftedNoiseDropsTheHorizontalShiftAndKeepsShiftY() {
        assertFoldMatchesVanilla(withShiftY(withLiveNoise(DensityFunctions.shiftedNoise2d(
                DensityFunctions.constant(SHIFT_X), DensityFunctions.constant(SHIFT_Z), XZ_SCALE, NOISE_DATA))));
    }

    @Test
    void shiftFoldsToTheSameSample() {
        assertFoldMatchesVanilla(withLiveNoise(DensityFunctions.shift(NOISE_DATA)));
    }

    @Test
    void shiftAKeepsTheConstantInSlotY() {
        assertFoldMatchesVanilla(withLiveNoise(DensityFunctions.shiftA(NOISE_DATA)));
    }

    @Test
    void shiftBSwapsTheAxesAndZeroesSlotZ() {
        assertFoldMatchesVanilla(withLiveNoise(DensityFunctions.shiftB(NOISE_DATA)));
    }

    @Test
    void everyCompiledShiftFunctionIsPeriodicInBothAxes() {
        Random random = new Random(SEED);
        for (DensityFunction source : List.of(
                withLiveNoise(DensityFunctions.shift(NOISE_DATA)),
                withLiveNoise(DensityFunctions.shiftA(NOISE_DATA)),
                withLiveNoise(DensityFunctions.shiftB(NOISE_DATA)))) {
            for (WorldLoopTransformer transformer : WORLDS) {
                AstNode folded = NoiseRouterBuild.withTransformer(transformer, () -> McToAst.toAst(source));
                int xWidth = transformer.coords.x.domainLength;
                int zWidth = transformer.coords.z.domainLength;

                for (int i = 0; i < PERIODICITY_SAMPLES; i++) {
                    int x = blockIn(random, transformer.coords.x);
                    int y = LOWEST_Y + random.nextInt(WORLD_HEIGHT);
                    int z = blockIn(random, transformer.coords.z);

                    assertEquals(sampleFolded(folded, x, y, z), sampleFolded(folded, x + xWidth, y, z),
                            "x lap of " + source + " in " + transformer + " at (" + x + ", " + y + ", " + z + ")");
                    assertEquals(sampleFolded(folded, x, y, z), sampleFolded(folded, x, y, z + zWidth),
                            "z lap of " + source + " in " + transformer + " at (" + x + ", " + y + ", " + z + ")");
                }
            }
        }
    }

    @Test
    void unknownDensityFunctionIsPassedThrough() {
        DensityFunction source = DensityFunctions.constant(1.0);
        AstNode produced = new ConstantNode(1.0);

        assertEquals(produced, C2meDfcAst.fold(source, produced));
    }

    @Test
    void unexpectedNodeShapeFailsTheCompile() {
        DensityFunction source = withLiveNoise(DensityFunctions.noise(NOISE_DATA, XZ_SCALE, Y_SCALE));

        assertThrows(IllegalStateException.class, () -> NoiseRouterBuild.withTransformer(WRAPPED,
                () -> C2meDfcAst.fold(source, new ConstantNode(0.0))));
    }

    @Test
    void routerThatWrapsNothingIsNotFolded() {
        DensityFunction source = withLiveNoise(DensityFunctions.noise(NOISE_DATA, XZ_SCALE, Y_SCALE));

        AstNode produced = McToAst.toAst(source);

        assertFalse(produced instanceof C2meFoldedNoiseNode,
                "a router built for a generator that wraps nothing must carry no toroidal node");
    }

    private static void assertFoldMatchesVanilla(DensityFunction source) {
        AstNode folded = NoiseRouterBuild.withTransformer(WRAPPED, () -> McToAst.toAst(source));
        DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(SAMPLE_X, SAMPLE_Y, SAMPLE_Z);

        double vanilla = GenerationTransformerContext.withTransformer(WRAPPED, () -> source.compute(at));
        double compiled = GenerationTransformerContext.withTransformer(WRAPPED,
                () -> sampleFolded(folded, SAMPLE_X, SAMPLE_Y, SAMPLE_Z));

        assertEquals(vanilla, compiled);
    }

    private static double sampleFolded(AstNode folded, int x, int y, int z) {
        AstNode node = folded;
        double amplitude = 1.0;
        if (node instanceof MulNode amplified) {
            amplitude = evaluate(amplified.right, x, y, z);
            node = amplified.left;
        }

        C2meFoldedNoiseNode fold = assertInstanceOf(C2meFoldedNoiseNode.class, node);

        return ContextScaledNoise.sampleWrapped(fold.transformer, fold.slotAxes, fold.noise,
                evaluate(fold.foldedX, x, y, z), evaluate(fold.foldedY, x, y, z), evaluate(fold.foldedZ, x, y, z),
                fold.horizontalScale)
                * amplitude;
    }

    private static double evaluate(AstNode node, int x, int y, int z) {
        return switch (node) {
            case ConstantNode constant -> constant.getValue();
            case CoordinateNode coordinate -> switch (coordinate.axis) {
                case X -> x;
                case Y -> y;
                case Z -> z;
            };
            case MulNode mul -> evaluate(mul.left, x, y, z) * evaluate(mul.right, x, y, z);
            case AddNode add -> evaluate(add.left, x, y, z) + evaluate(add.right, x, y, z);
            default -> throw new IllegalStateException("no interpreter for " + node.getClass().getName());
        };
    }

    private static int blockIn(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength);
    }

    private static DensityFunction withLiveNoise(DensityFunction function) {
        return function.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                return input;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
                return NOISE;
            }
        });
    }

    private static DensityFunction withShiftY(DensityFunction function) {
        DensityFunction zero = DensityFunctions.zero();

        return function.mapAll(input -> input == zero ? DensityFunctions.constant(SHIFT_Y) : input);
    }
}
