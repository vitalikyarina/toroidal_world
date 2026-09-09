package com.toroidalworld.compat.c2me;

import java.util.Objects;

import com.toroidalworld.core.WrapDomain;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;

public final class C2meWarpedAxisNode implements AstNode {
    public final CoordinateNode.Axis axis;
    public final WrapDomain domain;
    public final AstNode shift;
    public final double divisor;

    public C2meWarpedAxisNode(CoordinateNode.Axis axis, WrapDomain domain, AstNode shift, double divisor) {
        this.axis = Objects.requireNonNull(axis);
        this.domain = Objects.requireNonNull(domain);
        this.shift = Objects.requireNonNull(shift);
        this.divisor = divisor;
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[]{this.shift};
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        AstNode transformedShift = this.shift.transform(transformer);

        return transformer.transform(transformedShift == this.shift
                ? this
                : new C2meWarpedAxisNode(this.axis, this.domain, transformedShift, this.divisor));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        C2meWarpedAxisNode that = (C2meWarpedAxisNode) o;
        return this.axis == that.axis
                && this.domain == that.domain
                && Double.compare(this.divisor, that.divisor) == 0
                && this.shift.equals(that.shift);
    }

    @Override
    public int hashCode() {
        int result = this.axis.hashCode();
        result = 31 * result + System.identityHashCode(this.domain);
        result = 31 * result + Double.hashCode(this.divisor);
        return 31 * result + this.shift.hashCode();
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        C2meWarpedAxisNode that = (C2meWarpedAxisNode) o;
        return this.axis == that.axis
                && this.domain == that.domain
                && Double.compare(this.divisor, that.divisor) == 0
                && this.shift.relaxedEquals(that.shift);
    }

    @Override
    public int relaxedHashCode() {
        int result = this.axis.hashCode();
        result = 31 * result + System.identityHashCode(this.domain);
        result = 31 * result + Double.hashCode(this.divisor);
        return 31 * result + this.shift.relaxedHashCode();
    }
}
