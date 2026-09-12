package com.toroidalworld.engine;

import java.util.Arrays;

public final class DoubleStack {
    private static final int INITIAL_CAPACITY = 8;

    private double[] values = new double[INITIAL_CAPACITY];

    private int depth;

    public void push(double value) {
        if (this.depth == this.values.length) {
            this.values = Arrays.copyOf(this.values, this.depth * 2);
        }

        this.values[this.depth++] = value;
    }

    public double pop() {
        return this.values[--this.depth];
    }

    public boolean isEmpty() {
        return this.depth == 0;
    }
}
