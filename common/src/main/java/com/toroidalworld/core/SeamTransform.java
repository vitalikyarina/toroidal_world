package com.toroidalworld.core;

public record SeamTransform(int xSign, int zSign, int xShift, int zShift) {
    public static final SeamTransform IDENTITY = new SeamTransform(1, 1, 0, 0);

    public SeamTransform {
        if (Math.abs(xSign) != 1 || Math.abs(zSign) != 1) {
            throw new IllegalArgumentException("An axis sign is +1 or -1, got x=" + xSign + " z=" + zSign);
        }
    }

    public static SeamTransform translation(int xShift, int zShift) {
        return new SeamTransform(1, 1, xShift, zShift);
    }

    public static SeamTransform glideX(int xShift, int zShift) {
        return new SeamTransform(1, -1, xShift, zShift);
    }

    public static SeamTransform glideZ(int xShift, int zShift) {
        return new SeamTransform(-1, 1, xShift, zShift);
    }

    public boolean isIdentity() {
        return this.xSign == 1 && this.zSign == 1 && this.xShift == 0 && this.zShift == 0;
    }

    public FoldOrientation orientation() {
        return FoldOrientation.of(this.xSign < 0, this.zSign < 0);
    }

    public double applyX(double x) {
        return this.xSign * x + this.xShift;
    }

    public double applyZ(double z) {
        return this.zSign * z + this.zShift;
    }

    public int applyCellX(int x) {
        return this.xSign * x + this.xShift + (this.xSign < 0 ? -1 : 0);
    }

    public int applyCellZ(int z) {
        return this.zSign * z + this.zShift + (this.zSign < 0 ? -1 : 0);
    }

    public SeamTransform then(SeamTransform next) {
        return new SeamTransform(
                this.xSign * next.xSign,
                this.zSign * next.zSign,
                next.xSign * this.xShift + next.xShift,
                next.zSign * this.zShift + next.zShift);
    }

    public SeamTransform inverse() {
        return new SeamTransform(this.xSign, this.zSign, -this.xSign * this.xShift, -this.zSign * this.zShift);
    }

    public SeamTransform power(int exponent) {
        if (exponent == 0) {
            return IDENTITY;
        }

        if (exponent < 0) {
            return power(-exponent).inverse();
        }

        return new SeamTransform(
                signPower(this.xSign, exponent),
                signPower(this.zSign, exponent),
                shiftPower(this.xSign, this.xShift, exponent),
                shiftPower(this.zSign, this.zShift, exponent));
    }

    private static int signPower(int sign, int exponent) {
        return sign > 0 || (exponent & 1) == 0 ? 1 : -1;
    }

    private static int shiftPower(int sign, int shift, int exponent) {
        if (sign > 0) {
            return Math.multiplyExact(shift, exponent);
        }

        return (exponent & 1) == 0 ? 0 : shift;
    }
}
