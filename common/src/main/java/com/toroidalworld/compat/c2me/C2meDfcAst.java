package com.toroidalworld.compat.c2me;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.noise.NoiseConstants;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class C2meDfcAst {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final AstNode ORIGIN = new ConstantNode(0.0);

    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    public static AstNode fold(DensityFunction source, AstNode produced) {
        Fold fold = foldOf(source);
        if (fold == null) {
            reportOnce(source);
            return produced;
        }

        if (fold.amplified()) {
            if (!(produced instanceof MulNode amplified && amplified.left instanceof GenericShiftedNoiseNode noise)) {
                throw brokenShape(source, produced);
            }

            return new MulNode(foldNoise(noise, fold), amplified.right);
        }

        if (!(produced instanceof GenericShiftedNoiseNode noise)) {
            throw brokenShape(source, produced);
        }

        return foldNoise(noise, fold);
    }

    private static @Nullable Fold foldOf(DensityFunction source) {
        return switch (source) {
            case DensityFunctions.Noise noise ->
                    new Fold(CoordinateNode.AXIS_X, CoordinateNode.AXIS_Z, horizontalScale(noise), false);
            case DensityFunctions.ShiftedNoise shifted ->
                    new Fold(CoordinateNode.AXIS_X, CoordinateNode.AXIS_Z, shifted.xzScale(), false);
            case DensityFunctions.Shift _ ->
                    new Fold(CoordinateNode.AXIS_X, CoordinateNode.AXIS_Z, NoiseConstants.SHIFT_SCALE, true);
            case DensityFunctions.ShiftA _ ->
                    new Fold(CoordinateNode.AXIS_X, CoordinateNode.AXIS_Z, NoiseConstants.SHIFT_SCALE, true);
            case DensityFunctions.ShiftB _ ->
                    new Fold(CoordinateNode.AXIS_Z, ORIGIN, NoiseConstants.SHIFT_SCALE, true);
            default -> null;
        };
    }

    @SuppressWarnings("deprecation")
    private static double horizontalScale(DensityFunctions.Noise noise) {
        return noise.xzScale();
    }

    private static AstNode foldNoise(GenericShiftedNoiseNode noise, Fold fold) {
        return new C2meFoldedNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.noise,
                fold.rawX(), fold.rawZ(), fold.horizontalScale());
    }

    private static void reportOnce(DensityFunction source) {
        String type = source.getClass().getName();
        if (REPORTED.add(type)) {
            LOGGER.debug("[c2me-compat] dfc_ast not_folded type={}", type);
        }
    }

    private static IllegalStateException brokenShape(DensityFunction source, AstNode produced) {
        return new IllegalStateException("[c2me-compat] dfc_ast broken_shape type=" + source.getClass().getName()
                + " produced=" + produced.getClass().getName()
                + " — C2ME no longer compiles this function to the node the toroidal fold replaces");
    }

    private record Fold(AstNode rawX, AstNode rawZ, double horizontalScale, boolean amplified) {
    }

    private C2meDfcAst() {
    }
}
