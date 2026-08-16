package com.toroidalworld.compat.c2me;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

// Gives every noise node C2ME compiles a second reading of its own inputs — the folded one — at the moment the node is
// built.
//
// The horizontal scale is read back out of the tree rather than off the density function it came from: the five vanilla
// functions that reach this point are protected records, nameable from C2ME's package but not from this one, and
// widening them would take an access transformer per loader for a value the tree already states. What C2ME writes into
// the X and Z slots is the scale and the shift this mod's own mixins strip — coordinate times scale, optionally plus a
// shift — so stripping them back off is a local inversion of the expression standing right there, and a slot shaped any
// other way is left alone rather than guessed at.
public final class C2meDfcAst {
    private static final Logger LOGGER = LogUtils.getLogger();

    // The two shapes C2ME's frontend produces around a noise node: the node itself for Noise and ShiftedNoise, and the
    // node times four for the three Shift functions.
    public static AstNode fold(AstNode produced) {
        if (produced instanceof MulNode scaled && scaled.left instanceof GenericShiftedNoiseNode noise) {
            AstNode folded = foldNoise(noise);
            return folded == noise ? produced : new MulNode(folded, scaled.right);
        }

        if (produced instanceof GenericShiftedNoiseNode noise) {
            return foldNoise(noise);
        }

        return produced;
    }

    private static AstNode foldNoise(GenericShiftedNoiseNode noise) {
        if (noise instanceof C2meFoldedNoiseNode) {
            return noise;
        }

        Slot x = slot(noise.inputX);
        Slot z = slot(noise.inputZ);
        if (x == null || z == null || !x.agreesWith(z)) {
            LOGGER.warn("[c2me-compat] dfc_ast unfoldable inputX={} inputZ={}",
                    noise.inputX.getClass().getName(), noise.inputZ.getClass().getName());
            return noise;
        }

        double horizontalScale = x.scaled ? x.scale : z.scale;
        return new C2meFoldedNoiseNode(noise.inputX, noise.inputY, noise.inputZ, noise.noise,
                x.raw, z.raw, horizontalScale);
    }

    // What the slot would hand the noise once the fold takes it over: the coordinate alone. A horizontal shift warps
    // the sampling domain and breaks the phase of the wrapped noise, so it is dropped here exactly as
    // DensityFunctionsShiftedNoiseMixin drops it; a slot that is already a bare constant travels through untouched,
    // which is the third input of every ShiftB.
    private static @Nullable Slot slot(AstNode input) {
        if (input instanceof ConstantNode constant) {
            return new Slot(constant, false, 0.0);
        }

        if (input instanceof AddNode shifted) {
            Slot beforeShift = scaledCoordinate(shifted.left);
            return beforeShift != null ? beforeShift : scaledCoordinate(shifted.right);
        }

        return scaledCoordinate(input);
    }

    private static @Nullable Slot scaledCoordinate(AstNode input) {
        if (!(input instanceof MulNode scaled)) {
            return null;
        }

        if (scaled.left instanceof CoordinateNode coordinate && scaled.right instanceof ConstantNode scale) {
            return new Slot(coordinate, true, scale.getValue());
        }

        if (scaled.right instanceof CoordinateNode coordinate && scaled.left instanceof ConstantNode scale) {
            return new Slot(coordinate, true, scale.getValue());
        }

        return null;
    }

    // One scale is parked per sample and both horizontal axes read it, so two slots that disagree have no single
    // answer to park — the node keeps C2ME's reading alone rather than take one axis's scale for the other's.
    private record Slot(AstNode raw, boolean scaled, double scale) {
        boolean agreesWith(Slot other) {
            if (this.scaled && other.scaled) {
                return Double.compare(this.scale, other.scale) == 0;
            }

            return this.scaled || other.scaled;
        }
    }

    private C2meDfcAst() {
    }
}
