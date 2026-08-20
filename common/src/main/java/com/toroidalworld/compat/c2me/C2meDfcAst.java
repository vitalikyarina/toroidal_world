package com.toroidalworld.compat.c2me;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class C2meDfcAst {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static AstNode fold(DensityFunction source, AstNode produced) {
        // The cave sampler is handed back to vanilla rather than folded here. It divides the coordinate by a rarity the
        // function itself computes, so a fold has to sit inside that division — which DensityFunctionsWeirdScaledSamplerMixin
        // already does, and which C2ME's own emitter writes past by inlining the whole division. Compiling a second copy of
        // that fold would put the same statement in two places that must agree block for block, and a disagreement between
        // them shows up only as terrain that differs with C2ME's compiler on or off. So the node goes back to the
        // interpreter, exactly as C2ME does with the End islands, and the one fold stays the one fold. Costs the compiled
        // path for the three router uses on this game version; 26.2 retired the function, so main has nothing here.
        if (produced instanceof DFTWeirdScaledSamplerNode) {
            return new DelegateNode(source);
        }

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
