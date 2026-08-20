package com.toroidalworld.compat.c2me;

import java.util.Objects;

import com.toroidalworld.core.WorldLoopTransformer;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class C2meFoldedNoiseNode extends GenericShiftedNoiseNode {
    public final AstNode foldedX;
    public final AstNode foldedZ;

    public final double horizontalScale;

    public final WorldLoopTransformer transformer;

    public C2meFoldedNoiseNode(AstNode inputX, AstNode inputY, AstNode inputZ, DensityFunction.NoiseHolder noise,
            AstNode foldedX, AstNode foldedZ, double horizontalScale, WorldLoopTransformer transformer) {
        super(inputX, inputY, inputZ, noise);
        this.foldedX = Objects.requireNonNull(foldedX);
        this.foldedZ = Objects.requireNonNull(foldedZ);
        this.horizontalScale = horizontalScale;
        this.transformer = Objects.requireNonNull(transformer);
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
                        transformedFoldedX, transformedFoldedZ, this.horizontalScale, this.transformer));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        C2meFoldedNoiseNode that = (C2meFoldedNoiseNode) o;
        return Double.compare(this.horizontalScale, that.horizontalScale) == 0
                && this.transformer == that.transformer
                && this.foldedX.equals(that.foldedX)
                && this.foldedZ.equals(that.foldedZ);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.foldedX.hashCode();
        result = 31 * result + this.foldedZ.hashCode();
        result = 31 * result + Double.hashCode(this.horizontalScale);
        return 31 * result + System.identityHashCode(this.transformer);
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (!super.relaxedEquals(o)) {
            return false;
        }

        C2meFoldedNoiseNode that = (C2meFoldedNoiseNode) o;
        return Double.compare(this.horizontalScale, that.horizontalScale) == 0
                && this.transformer == that.transformer
                && this.foldedX.relaxedEquals(that.foldedX)
                && this.foldedZ.relaxedEquals(that.foldedZ);
    }

    @Override
    public int relaxedHashCode() {
        int result = super.relaxedHashCode();
        result = 31 * result + this.foldedX.relaxedHashCode();
        result = 31 * result + this.foldedZ.relaxedHashCode();
        result = 31 * result + Double.hashCode(this.horizontalScale);
        return 31 * result + System.identityHashCode(this.transformer);
    }
}
