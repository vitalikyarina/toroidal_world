package com.toroidalworld.core;

public record ForeignSpan(int min, int max) {
    public ForeignSpan {
        if (max <= min) {
            throw new IllegalArgumentException("A foreign span is never empty, got [" + min + ", " + max + ")");
        }
    }

    public boolean contains(int coord) {
        return coord >= this.min && coord < this.max;
    }

    public boolean contains(double coord) {
        return coord >= this.min && coord < this.max;
    }

    public ForeignSpan scaled(int unit) {
        return new ForeignSpan(Math.multiplyExact(this.min, unit), Math.multiplyExact(this.max, unit));
    }
}
