package com.toroidalworld.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.mojang.serialization.Lifecycle;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;

class TerrainCeilingTest {
    private static final String JAGGEDNESS_PATH = "overworld/jaggedness";

    private static final String FOREIGN_PATH = "overworld/factor";

    private static final int SURFACE_Y = 64;

    private static final double BASE_BLOCKS = 40.0;

    private static final double RAMP_BLOCKS = 16.0;

    private static final double BLOCKS_PER_DENSITY_UNIT = 128.0;

    private static final double NOISE_MAX = 1.0;

    private static final double BASE_DENSITY = 4.0;

    private static final double PENALTY = 0.25;

    private static final double TOLERANCE = 1.0e-9;

    private static double densityAt(NoiseRouter router, int y) {
        return TerrainCeiling.withCeiling(router).finalDensity()
                .compute(new DensityFunction.SinglePointContext(0, y, 0));
    }

    private static NoiseRouter routerWithJaggedness(double splineValue) {
        return router(jaggednessProduct(JAGGEDNESS_PATH, splineValue));
    }

    private static DensityFunction jaggednessProduct(String path, double splineValue) {
        MappedRegistry<DensityFunction> functions =
                new MappedRegistry<>(Registries.DENSITY_FUNCTION, Lifecycle.stable());
        Holder.Reference<DensityFunction> spline = functions.register(
                ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(path)),
                DensityFunctions.constant(splineValue),
                RegistrationInfo.BUILT_IN);
        functions.freeze();
        return DensityFunctions.flatCache(DensityFunctions.mul(new DensityFunctions.HolderHolder(spline),
                DensityFunctions.constant(NOISE_MAX)));
    }

    private static NoiseRouter router(DensityFunction jaggedness) {
        DensityFunction zero = DensityFunctions.zero();
        DensityFunction finalDensity =
                DensityFunctions.add(DensityFunctions.constant(BASE_DENSITY), jaggedness);
        return new NoiseRouter(zero, zero, zero, zero, zero, zero, zero, zero, zero, zero,
                DensityFunctions.yClampedGradient(SURFACE_Y, SURFACE_Y + 1, 1.0, 0.0),
                finalDensity, zero, zero, zero);
    }

    private static double undisturbed(double splineValue) {
        return BASE_DENSITY + splineValue * NOISE_MAX;
    }

    private static int ceilingY(double splineValue) {
        return (int) (SURFACE_Y + BASE_BLOCKS + BLOCKS_PER_DENSITY_UNIT * NOISE_MAX * splineValue);
    }

    private static int maxNodeCount(DensityFunction root) {
        Deque<DensityFunction> pending = new ArrayDeque<>();
        Set<DensityFunction> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(root);
        seen.add(root);
        int found = 0;

        while (!pending.isEmpty()) {
            DensityFunction node = pending.remove();
            if (node instanceof DensityFunctions.TwoArgumentSimpleFunction candidate
                    && candidate.type() == DensityFunctions.TwoArgumentSimpleFunction.Type.MAX) {
                found++;
            }

            if (node instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder) && !holder.isBound()) {
                continue;
            }

            for (DensityFunction child : DensityFunctionChildren.of(node)) {
                if (seen.add(child)) {
                    pending.add(child);
                }
            }
        }

        return found;
    }

    @Nested
    class NothingToStandOn {
        @Test
        void aRouterWithoutAJaggednessNodeIsHandedBackUnchanged() {
            NoiseRouter source = router(DensityFunctions.constant(0.0));
            assertSame(source, TerrainCeiling.withCeiling(source));
        }

        @Test
        void aProductOverAnotherRegisteredFunctionIsNotJaggedness() {
            NoiseRouter source = router(jaggednessProduct(FOREIGN_PATH, 0.0));
            assertSame(source, TerrainCeiling.withCeiling(source));
        }
    }

    @Nested
    class BelowTheCeiling {
        @Test
        void theDensityAtTheCeilingIsTheDensityVanillaWouldHaveGiven() {
            assertEquals(undisturbed(0.0), densityAt(routerWithJaggedness(0.0), ceilingY(0.0)), TOLERANCE);
        }

        @Test
        void theDensityWellBelowTheCeilingIsUntouched() {
            assertEquals(undisturbed(0.0), densityAt(routerWithJaggedness(0.0), SURFACE_Y), TOLERANCE);
        }
    }

    @Nested
    class AboveTheCeiling {
        @Test
        void thePenaltyIsFullOnceTheRampIsWalked() {
            assertEquals(undisturbed(0.0) - PENALTY,
                    densityAt(routerWithJaggedness(0.0), ceilingY(0.0) + (int) RAMP_BLOCKS), TOLERANCE);
        }

        @Test
        void thePenaltyIsHalfwayUpTheRampAtItsMiddle() {
            assertEquals(undisturbed(0.0) - PENALTY / 2.0,
                    densityAt(routerWithJaggedness(0.0), ceilingY(0.0) + (int) RAMP_BLOCKS / 2), TOLERANCE);
        }

        @Test
        void thePenaltyDoesNotGrowPastTheRamp() {
            assertEquals(undisturbed(0.0) - PENALTY,
                    densityAt(routerWithJaggedness(0.0), ceilingY(0.0) + 4 * (int) RAMP_BLOCKS), TOLERANCE);
        }
    }

    @Nested
    class JaggednessLiftsTheCeiling {
        @Test
        void aJaggedColumnKeepsTheDensityWhereAFlatOneWouldHaveLostIt() {
            double jagged = 0.5;
            int y = ceilingY(0.0) + (int) RAMP_BLOCKS;
            assertEquals(undisturbed(jagged), densityAt(routerWithJaggedness(jagged), y), TOLERANCE);
        }

        @Test
        void theLiftIsOneHundredAndTwentyEightBlocksPerUnitOfJaggedness() {
            double jagged = 0.5;
            assertEquals(undisturbed(jagged), densityAt(routerWithJaggedness(jagged), ceilingY(jagged)),
                    TOLERANCE);
        }

        @Test
        void aJaggedColumnStillLosesItAboveItsOwnCeiling() {
            double jagged = 0.5;
            assertEquals(undisturbed(jagged) - PENALTY,
                    densityAt(routerWithJaggedness(jagged), ceilingY(jagged) + (int) RAMP_BLOCKS), TOLERANCE);
        }
    }

    @Nested
    class TheHeadroomNode {
        @Test
        void aSplineThatCannotGoNegativeIsNotWrappedInAMax() {
            NoiseRouter ceilinged = TerrainCeiling.withCeiling(routerWithJaggedness(0.5));
            assertEquals(0, maxNodeCount(ceilinged.finalDensity()));
        }

        @Test
        void aSplineThatCanGoNegativeNeverLowersTheCeiling() {
            double negative = -0.5;
            assertEquals(undisturbed(negative),
                    densityAt(routerWithJaggedness(negative), ceilingY(0.0)), TOLERANCE);
        }
    }
}
