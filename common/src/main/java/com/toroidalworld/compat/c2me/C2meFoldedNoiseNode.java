package com.toroidalworld.compat.c2me;

import java.util.Objects;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.SlotAxes;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class C2meFoldedNoiseNode extends GenericShiftedNoiseNode {
    public final AstNode foldedX;
    public final AstNode foldedY;
    public final AstNode foldedZ;

    public final SlotAxes slotAxes;

    public final double horizontalScale;

    public final double verticalShare;

    public final WorldFold transformer;

    public C2meFoldedNoiseNode(AstNode inputX, AstNode inputY, AstNode inputZ, DensityFunction.NoiseHolder noise,
            AstNode foldedX, AstNode foldedY, AstNode foldedZ, SlotAxes slotAxes,
            double horizontalScale, double verticalShare, WorldFold transformer) {
        super(inputX, inputY, inputZ, noise);
        this.foldedX = Objects.requireNonNull(foldedX);
        this.foldedY = Objects.requireNonNull(foldedY);
        this.foldedZ = Objects.requireNonNull(foldedZ);
        this.slotAxes = Objects.requireNonNull(slotAxes);
        this.horizontalScale = horizontalScale;
        this.verticalShare = verticalShare;
        this.transformer = Objects.requireNonNull(transformer);
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{this.inputX, this.inputY, this.inputZ, this.foldedX, this.foldedY, this.foldedZ};
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode transformedInputX = this.inputX.transform(transformer);
        AstNode transformedInputY = this.inputY.transform(transformer);
        AstNode transformedInputZ = this.inputZ.transform(transformer);
        AstNode transformedFoldedX = this.foldedX.transform(transformer);
        AstNode transformedFoldedY = this.foldedY.transform(transformer);
        AstNode transformedFoldedZ = this.foldedZ.transform(transformer);
        boolean unchanged = transformedInputX == this.inputX
                && transformedInputY == this.inputY
                && transformedInputZ == this.inputZ
                && transformedFoldedX == this.foldedX
                && transformedFoldedY == this.foldedY
                && transformedFoldedZ == this.foldedZ;

        return transformer.transform(unchanged
                ? this
                : new C2meFoldedNoiseNode(transformedInputX, transformedInputY, transformedInputZ, this.noise,
                        transformedFoldedX, transformedFoldedY, transformedFoldedZ, this.slotAxes,
                        this.horizontalScale, this.verticalShare, this.transformer));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        C2meFoldedNoiseNode that = (C2meFoldedNoiseNode) o;
        return Double.compare(this.horizontalScale, that.horizontalScale) == 0
                && Double.compare(this.verticalShare, that.verticalShare) == 0
                && this.transformer == that.transformer
                && this.slotAxes.equals(that.slotAxes)
                && this.foldedX.equals(that.foldedX)
                && this.foldedY.equals(that.foldedY)
                && this.foldedZ.equals(that.foldedZ);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.foldedX.hashCode();
        result = 31 * result + this.foldedY.hashCode();
        result = 31 * result + this.foldedZ.hashCode();
        result = 31 * result + this.slotAxes.hashCode();
        result = 31 * result + Double.hashCode(this.horizontalScale);
        result = 31 * result + Double.hashCode(this.verticalShare);
        return 31 * result + System.identityHashCode(this.transformer);
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (!super.relaxedEquals(o)) {
            return false;
        }

        C2meFoldedNoiseNode that = (C2meFoldedNoiseNode) o;
        return Double.compare(this.horizontalScale, that.horizontalScale) == 0
                && Double.compare(this.verticalShare, that.verticalShare) == 0
                && this.transformer == that.transformer
                && this.slotAxes.equals(that.slotAxes)
                && this.foldedX.relaxedEquals(that.foldedX)
                && this.foldedY.relaxedEquals(that.foldedY)
                && this.foldedZ.relaxedEquals(that.foldedZ);
    }

    @Override
    public int relaxedHashCode() {
        int result = super.relaxedHashCode();
        result = 31 * result + this.foldedX.relaxedHashCode();
        result = 31 * result + this.foldedY.relaxedHashCode();
        result = 31 * result + this.foldedZ.relaxedHashCode();
        result = 31 * result + this.slotAxes.hashCode();
        result = 31 * result + Double.hashCode(this.horizontalScale);
        result = 31 * result + Double.hashCode(this.verticalShare);
        return 31 * result + System.identityHashCode(this.transformer);
    }
}
