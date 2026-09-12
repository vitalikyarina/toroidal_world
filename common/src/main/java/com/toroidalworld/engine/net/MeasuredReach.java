package com.toroidalworld.engine.net;

import com.toroidalworld.engine.DoubleStack;

public final class MeasuredReach implements AutoCloseable {
    public static final double UNMEASURED = -1.0;

    private static final ThreadLocal<MeasuredReach> CURRENT = ThreadLocal.withInitial(MeasuredReach::new);

    private double blocks = UNMEASURED;

    private final DoubleStack previous = new DoubleStack();

    public static double blocks() {
        return CURRENT.get().blocks;
    }

    public static MeasuredReach measuring(double blocks) {
        MeasuredReach scope = CURRENT.get();
        scope.push(blocks);
        return scope;
    }

    private void push(double measured) {
        this.previous.push(this.blocks);
        this.blocks = measured;
    }

    @Override
    public void close() {
        this.blocks = this.previous.pop();
    }

    private MeasuredReach() {
    }
}
