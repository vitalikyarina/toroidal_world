package com.toroidalworld.engine.net;

import java.util.Arrays;

public final class MeasuredReach implements AutoCloseable {
    public static final double UNMEASURED = -1.0;

    private static final ThreadLocal<MeasuredReach> CURRENT = ThreadLocal.withInitial(MeasuredReach::new);

    private double blocks = UNMEASURED;

    private double[] previous = new double[8];

    private int depth;

    public static double blocks() {
        return CURRENT.get().blocks;
    }

    public static MeasuredReach measuring(double blocks) {
        MeasuredReach scope = CURRENT.get();
        scope.push(blocks);
        return scope;
    }

    private void push(double measured) {
        if (this.depth == this.previous.length) {
            this.previous = Arrays.copyOf(this.previous, this.depth * 2);
        }

        this.previous[this.depth++] = this.blocks;
        this.blocks = measured;
    }

    @Override
    public void close() {
        this.blocks = this.previous[--this.depth];
    }

    private MeasuredReach() {
    }
}
