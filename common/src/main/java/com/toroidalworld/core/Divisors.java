package com.toroidalworld.core;

public final class Divisors {
    public static int[] of(int value) {
        int count = 0;
        for (int candidate = 1; (long) candidate * candidate <= value; candidate++) {
            if (value % candidate == 0) {
                count += (long) candidate * candidate == value ? 1 : 2;
            }
        }

        int[] divisors = new int[count];
        int lower = 0;
        int upper = count - 1;
        for (int candidate = 1; (long) candidate * candidate <= value; candidate++) {
            if (value % candidate != 0) {
                continue;
            }

            divisors[lower++] = candidate;
            int paired = value / candidate;
            if (paired != candidate) {
                divisors[upper--] = paired;
            }
        }

        return divisors;
    }

    private Divisors() {
    }
}
