package com.toroidalworld.compat.c2me;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.engine.noise.DensityFunctionSlotAxes;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.toroidalworld.engine.noise.NoiseConstants;
import com.toroidalworld.engine.noise.SlotAxes;
import com.toroidalworld.engine.noise.SlotAxis;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class C2meDfcAst {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static AstNode fold(DensityFunction source, AstNode produced) {
        // The cave sampler goes back to the interpreter instead of being folded here: it divides the coordinate by a
        // rarity it computes itself, so the fold has to sit inside that division — which
        // DensityFunctionsWeirdScaledSamplerMixin already does and C2ME's emitter writes past by inlining the whole
        // division. A second copy compiled here would have to agree with that one block for block, and a disagreement
        // shows only as terrain that differs with C2ME on or off. 26.2 retired the function, so main has nothing here.
        if (produced instanceof DFTWeirdScaledSamplerNode) {
            return new DelegateNode(source);
        }

        Fold fold = foldOf(source);
        if (fold == null) {
            if (produced instanceof GenericShiftedNoiseNode && !delegatesToItsInput(source)) {
                LOGGER.warn("[c2me-compat] dfc_ast noise_not_folded type={}", source.getClass().getName());
            }

            return produced;
        }

        WorldFold transformer = GenerationTransformerContext.context().routerBuildTransformer();
        if (transformer == null) {
            return produced;
        }

        if (fold.amplified()) {
            if (!(produced instanceof MulNode amplified && amplified.left instanceof GenericShiftedNoiseNode noise)) {
                throw brokenShape(source, produced);
            }

            return new MulNode(foldNoise(source, noise, fold, transformer), amplified.right);
        }

        if (!(produced instanceof GenericShiftedNoiseNode noise)) {
            throw brokenShape(source, produced);
        }

        return foldNoise(source, noise, fold, transformer);
    }

    private static @Nullable Fold foldOf(DensityFunction source) {
        return switch (source) {
            case DensityFunctions.Noise noise -> noiseFold(noise);
            case DensityFunctions.ShiftedNoise shifted -> new Fold(SlotAxes.DEFAULT, shifted.xzScale(),
                    GenerationTransformerContext.verticalShare(shifted.xzScale(), shifted.yScale()), false,
                    shifted.xzScale() != 0.0);
            case DensityFunctions.Shift shift -> new Fold(SlotAxes.DEFAULT, NoiseConstants.SHIFT_SCALE,
                    GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE, true, false);
            case DensityFunctions.ShiftA shiftA -> new Fold(SlotAxes.DEFAULT, NoiseConstants.SHIFT_SCALE,
                    GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE, true, false);
            case DensityFunctions.ShiftB shiftB -> new Fold(DensityFunctionSlotAxes.SHIFT_B, NoiseConstants.SHIFT_SCALE,
                    GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE, true, false);
            default -> null;
        };
    }

    private static boolean delegatesToItsInput(DensityFunction source) {
        return source instanceof DensityFunctions.HolderHolder;
    }

    @SuppressWarnings("deprecation")
    private static Fold noiseFold(DensityFunctions.Noise noise) {
        return new Fold(SlotAxes.DEFAULT, noise.xzScale(),
                GenerationTransformerContext.verticalShare(noise.xzScale(), noise.yScale()), false, false);
    }

    private static AstNode foldNoise(DensityFunction source, GenericShiftedNoiseNode noise, Fold fold,
            WorldFold transformer) {
        SlotAxes axes = fold.axes();
        AstNode foldedX = fold.warped()
                ? warpedSlot(source, CoordinateNode.Axis.X, axes.x().domainOf(transformer), noise.inputX,
                        fold.horizontalScale())
                : slotNode(axes.x(), noise.inputX);
        AstNode foldedZ = fold.warped()
                ? warpedSlot(source, CoordinateNode.Axis.Z, axes.z().domainOf(transformer), noise.inputZ,
                        fold.horizontalScale())
                : slotNode(axes.z(), noise.inputZ);

        return new C2meFoldedNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.noise,
                foldedX, slotNode(axes.y(), noise.inputY), foldedZ,
                axes, fold.horizontalScale(), fold.verticalShare(), transformer);
    }

    private static AstNode slotNode(SlotAxis axis, AstNode ownInput) {
        return switch (axis) {
            case X -> CoordinateNode.AXIS_X;
            case Z -> CoordinateNode.AXIS_Z;
            case NONE -> ownInput;
        };
    }

    private static AstNode warpedSlot(DensityFunction source, CoordinateNode.Axis axis, WrapDomain domain,
            AstNode ownInput, double xzScale) {
        if (!(ownInput instanceof AddNode shifted
                && shifted.left instanceof MulNode scaled
                && scaled.left instanceof CoordinateNode coordinate
                && coordinate.axis == axis)) {
            throw brokenShape(source, ownInput);
        }

        return new C2meWarpedAxisNode(axis, domain, shifted.right, xzScale);
    }

    private static IllegalStateException brokenShape(DensityFunction source, AstNode produced) {
        return new IllegalStateException("[c2me-compat] dfc_ast broken_shape type=" + source.getClass().getName()
                + " produced=" + produced.getClass().getName()
                + " — C2ME no longer compiles this function to the node the toroidal fold replaces");
    }

    private record Fold(SlotAxes axes, double horizontalScale, double verticalShare, boolean amplified,
            boolean warped) {
    }

    private C2meDfcAst() {
    }
}
