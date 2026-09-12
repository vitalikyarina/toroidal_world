package com.toroidalworld.engine;

import java.util.Arrays;

public final class ObjectStack<T> {
    private static final int INITIAL_CAPACITY = 8;

    private Object[] values = new Object[INITIAL_CAPACITY];

    private int depth;

    public void push(T value) {
        if (this.depth == this.values.length) {
            this.values = Arrays.copyOf(this.values, this.depth * 2);
        }

        this.values[this.depth++] = value;
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        return (T) this.values[--this.depth];
    }
}
