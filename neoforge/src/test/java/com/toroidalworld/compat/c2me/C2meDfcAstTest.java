package com.toroidalworld.compat.c2me;

import static com.toroidalworld.noise.DensityFunctionFixture.CLIMATE_AMPLITUDES;
import static com.toroidalworld.noise.DensityFunctionFixture.CLIMATE_FIRST_OCTAVE;
import static com.toroidalworld.noise.DensityFunctionFixture.CLIMATE_NOISE_DATA;
import static com.toroidalworld.noise.DensityFunctionFixture.CLIMATE_XZ_SCALE;
import static com.toroidalworld.noise.DensityFunctionFixture.NOISE_DATA;
import static com.toroidalworld.noise.DensityFunctionFixture.SEED;
import static com.toroidalworld.noise.DensityFunctionFixture.SQUARE;
import static com.toroidalworld.noise.DensityFunctionFixture.WORLDS;
import static com.toroidalworld.noise.DensityFunctionFixture.blockIn;
import static com.toroidalworld.noise.DensityFunctionFixture.blockY;
import static com.toroidalworld.noise.DensityFunctionFixture.withClimateNoise;
import static com.toroidalworld.noise.DensityFunctionFixture.withLiveNoise;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.ClimateScaleCompression;
import com.toroidalworld.noise.ContextScaledNoise;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

class C2meDfcAstTest {
    private static final int PERIODICITY_SAMPLES = 32;

    private static final int SAMPLE_X = 137;
    private static final int SAMPLE_Y = 61;
    private static final int SAMPLE_Z = -211;

    private static final double XZ_SCALE = 0.7;
    private static final double Y_SCALE = 0.3;
    private static final double SHIFT_X = 3.0;
    private static final double SHIFT_Y = 7.0;
    private static final double SHIFT_Z = -5.0;

    private static final double FLAT_Y_SCALE = 0.0;

    private static final int GRID_SAMPLES = 16;

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
    void climateShapedShiftedNoiseFoldsToTheSameSample() {
        assertCorrectionIsLive(SQUARE);
        assertFoldMatchesVanillaOverGrid(withClimateNoise(DensityFunctions.shiftedNoise2d(
                DensityFunctions.constant(SHIFT_X), DensityFunctions.constant(SHIFT_Z),
                CLIMATE_XZ_SCALE, CLIMATE_NOISE_DATA)), SQUARE);
    }

    @Test
    void climateShapedNoiseFoldsToTheSameSample() {
        assertCorrectionIsLive(SQUARE);
        assertFoldMatchesVanillaOverGrid(
                withClimateNoise(DensityFunctions.noise(CLIMATE_NOISE_DATA, CLIMATE_XZ_SCALE, FLAT_Y_SCALE)),
                SQUARE);
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
            for (WorldFold transformer : WORLDS) {
                AstNode folded = GenerationTransformerContext.withRouterBuild(transformer, () -> McToAst.toAst(source));
                int xWidth = transformer.blockDomain(Direction.Axis.X).domainLength;
                int zWidth = transformer.blockDomain(Direction.Axis.Z).domainLength;

                for (int i = 0; i < PERIODICITY_SAMPLES; i++) {
                    int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                    int y = blockY(random);
                    int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));

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

        assertThrows(IllegalStateException.class, () -> GenerationTransformerContext.withRouterBuild(SQUARE,
                () -> C2meDfcAst.fold(source, new ConstantNode(0.0))));
    }

    @Test
    void routerThatWrapsNothingIsNotFolded() {
        DensityFunction source = withLiveNoise(DensityFunctions.noise(NOISE_DATA, XZ_SCALE, Y_SCALE));

        AstNode produced = McToAst.toAst(source);

        assertFalse(produced instanceof C2meFoldedNoiseNode,
                "a router built for a generator that wraps nothing must carry no toroidal node");
    }

    private static void assertCorrectionIsLive(WorldFold fold) {
        double compression = ClimateScaleCompression.factor(fold.blockDomain(Direction.Axis.X),
                fold.blockDomain(Direction.Axis.Z), CLIMATE_AMPLITUDES, Math.pow(2.0, CLIMATE_FIRST_OCTAVE),
                CLIMATE_XZ_SCALE, FLAT_Y_SCALE / CLIMATE_XZ_SCALE);

        assertTrue(compression > 1.0,
                "the climate fixture sits outside the compressed regime in " + fold + ", so the case proves nothing");
    }

    private static void assertFoldMatchesVanillaOverGrid(DensityFunction source, WorldFold fold) {
        AstNode folded = GenerationTransformerContext.withRouterBuild(fold, () -> McToAst.toAst(source));
        Random random = new Random(SEED);

        for (int i = 0; i < GRID_SAMPLES; i++) {
            int x = blockIn(random, fold.blockDomain(Direction.Axis.X));
            int y = blockY(random);
            int z = blockIn(random, fold.blockDomain(Direction.Axis.Z));
            DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(x, y, z);

            double vanilla = GenerationTransformerContext.withTransformer(fold, () -> source.compute(at));
            double compiled = GenerationTransformerContext.withTransformer(fold,
                    () -> sampleFolded(folded, x, y, z));

            assertEquals(vanilla, compiled, "at (" + x + ", " + y + ", " + z + ") in " + fold);
        }
    }

    private static void assertFoldMatchesVanilla(DensityFunction source) {
        AstNode folded = GenerationTransformerContext.withRouterBuild(SQUARE, () -> McToAst.toAst(source));
        DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(SAMPLE_X, SAMPLE_Y, SAMPLE_Z);

        double vanilla = GenerationTransformerContext.withTransformer(SQUARE, () -> source.compute(at));
        double compiled = GenerationTransformerContext.withTransformer(SQUARE,
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
                fold.horizontalScale, fold.verticalShare)
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

    private static DensityFunction withShiftY(DensityFunction function) {
        DensityFunction zero = DensityFunctions.zero();

        return function.mapAll(input -> input == zero ? DensityFunctions.constant(SHIFT_Y) : input);
    }
}
