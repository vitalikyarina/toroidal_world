package com.toroidalworld.compat.c2me;

import java.util.Objects;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;

// A noise node with both readings of the same sample: C2ME's own, which is what a world that does not wrap must keep
// getting, and this mod's, which hands the noise raw block coordinates and lets the horizontal scale travel through
// the generation context. The compiled method picks between them per call, the way the wrapped compute methods pick
// between their two branches.
//
// The inherited inputX/inputY/inputZ are C2ME's tree, untouched, so its own emitter can be handed this node as-is for
// the vanilla branch. The folded tree reuses that same inputY — the vertical coordinate is scaled and shifted
// identically either way — and replaces X and Z with the raw coordinate that was being scaled, which is exactly what
// DensityFunctionsNoiseMixin and DensityFunctionsShiftedNoiseMixin do to the same functions when C2ME is absent.
//
// It extends C2ME's node rather than standing beside it because TreeUtils.isNonTrivial asks by instanceof, and a
// noise sample that answers "trivial" there loses the caches C2ME would otherwise keep around it.
public final class C2meFoldedNoiseNode extends GenericShiftedNoiseNode {
    public final AstNode foldedX;
    public final AstNode foldedZ;

    // Not a child of the tree: it is one number per node, known when the AST is built, and the emitter writes it into
    // the call as a constant. It takes part in equality all the same — two nodes identical but for their scale are two
    // different fields, and letting them compare equal would collapse them onto one compiled method.
    public final double horizontalScale;

    public C2meFoldedNoiseNode(AstNode inputX, AstNode inputY, AstNode inputZ, DensityFunction.NoiseHolder noise,
            AstNode foldedX, AstNode foldedZ, double horizontalScale) {
        super(inputX, inputY, inputZ, noise);
        this.foldedX = Objects.requireNonNull(foldedX);
        this.foldedZ = Objects.requireNonNull(foldedZ);
        this.horizontalScale = horizontalScale;
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{this.inputX, this.inputY, this.inputZ, this.foldedX, this.foldedZ};
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode transformedInputX = this.inputX.transform(transformer);
        AstNode transformedInputY = this.inputY.transform(transformer);
        AstNode transformedInputZ = this.inputZ.transform(transformer);
        AstNode transformedFoldedX = this.foldedX.transform(transformer);
        AstNode transformedFoldedZ = this.foldedZ.transform(transformer);
        boolean unchanged = transformedInputX == this.inputX
                && transformedInputY == this.inputY
                && transformedInputZ == this.inputZ
                && transformedFoldedX == this.foldedX
                && transformedFoldedZ == this.foldedZ;

        return transformer.transform(unchanged
                ? this
                : new C2meFoldedNoiseNode(transformedInputX, transformedInputY, transformedInputZ, this.noise,
                        transformedFoldedX, transformedFoldedZ, this.horizontalScale));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        C2meFoldedNoiseNode that = (C2meFoldedNoiseNode) o;
        return Double.compare(this.horizontalScale, that.horizontalScale) == 0
                && this.foldedX.equals(that.foldedX)
                && this.foldedZ.equals(that.foldedZ);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.foldedX.hashCode();
        result = 31 * result + this.foldedZ.hashCode();
        return 31 * result + Double.hashCode(this.horizontalScale);
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (!super.relaxedEquals(o)) {
            return false;
        }

        C2meFoldedNoiseNode that = (C2meFoldedNoiseNode) o;
        return Double.compare(this.horizontalScale, that.horizontalScale) == 0
                && this.foldedX.relaxedEquals(that.foldedX)
                && this.foldedZ.relaxedEquals(that.foldedZ);
    }

    @Override
    public int relaxedHashCode() {
        int result = super.relaxedHashCode();
        result = 31 * result + this.foldedX.relaxedHashCode();
        result = 31 * result + this.foldedZ.relaxedHashCode();
        return 31 * result + Double.hashCode(this.horizontalScale);
    }
}
