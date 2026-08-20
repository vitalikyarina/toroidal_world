package com.toroidalworld.compat.c2me;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.DensityFunctionSlotAxes;
import com.toroidalworld.noise.NoiseConstants;
import com.toroidalworld.noise.NoiseRouterBuild;
import com.toroidalworld.noise.SlotAxes;
import com.toroidalworld.noise.SlotAxis;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class C2meDfcAst {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    public static AstNode fold(DensityFunction source, AstNode produced) {
        Fold fold = foldOf(source);
        if (fold == null) {
            reportOnce(source);
            return produced;
        }

        WorldLoopTransformer transformer = NoiseRouterBuild.wrappedTransformer();
        if (transformer == null) {
            return produced;
        }

        if (fold.amplified()) {
            if (!(produced instanceof MulNode amplified && amplified.left instanceof GenericShiftedNoiseNode noise)) {
                throw brokenShape(source, produced);
            }

            return new MulNode(foldNoise(noise, fold, transformer), amplified.right);
        }

        if (!(produced instanceof GenericShiftedNoiseNode noise)) {
            throw brokenShape(source, produced);
        }

        return foldNoise(noise, fold, transformer);
    }

    private static @Nullable Fold foldOf(DensityFunction source) {
        return switch (source) {
            case DensityFunctions.Noise noise -> new Fold(SlotAxes.DEFAULT, horizontalScale(noise), false);
            case DensityFunctions.ShiftedNoise shifted -> new Fold(SlotAxes.DEFAULT, shifted.xzScale(), false);
            case DensityFunctions.Shift _ -> new Fold(SlotAxes.DEFAULT, NoiseConstants.SHIFT_SCALE, true);
            case DensityFunctions.ShiftA _ -> new Fold(SlotAxes.DEFAULT, NoiseConstants.SHIFT_SCALE, true);
            case DensityFunctions.ShiftB _ ->
                    new Fold(DensityFunctionSlotAxes.SHIFT_B, NoiseConstants.SHIFT_SCALE, true);
            default -> null;
        };
    }

    @SuppressWarnings("deprecation")
    private static double horizontalScale(DensityFunctions.Noise noise) {
        return noise.xzScale();
    }

    private static AstNode foldNoise(GenericShiftedNoiseNode noise, Fold fold, WorldLoopTransformer transformer) {
        SlotAxes axes = fold.axes();

        return new C2meFoldedNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.noise,
                slotNode(axes.x(), noise.inputX), slotNode(axes.y(), noise.inputY), slotNode(axes.z(), noise.inputZ),
                axes, fold.horizontalScale(), transformer);
    }

    private static AstNode slotNode(SlotAxis axis, AstNode ownInput) {
        return switch (axis) {
            case X -> CoordinateNode.AXIS_X;
            case Z -> CoordinateNode.AXIS_Z;
            case NONE -> ownInput;
        };
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

    private record Fold(SlotAxes axes, double horizontalScale, boolean amplified) {
    }

    private C2meDfcAst() {
    }
}
