package com.toroidalworld.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DoubleStackTest {
    private static final int PAST_INITIAL_CAPACITY = 9;

    @Test
    void aFreshStackIsEmpty() {
        assertTrue(new DoubleStack().isEmpty());
    }

    @Test
    void popReturnsTheLastPushedValue() {
        DoubleStack stack = new DoubleStack();
        stack.push(1.5);
        stack.push(2.5);

        assertEquals(2.5, stack.pop());
        assertEquals(1.5, stack.pop());
    }

    @Test
    void aPushedStackIsNotEmptyUntilItIsPoppedBack() {
        DoubleStack stack = new DoubleStack();
        stack.push(1.5);
        assertFalse(stack.isEmpty());

        stack.pop();

        assertTrue(stack.isEmpty());
    }

    @Test
    void growingPastTheInitialCapacityKeepsEveryValue() {
        DoubleStack stack = new DoubleStack();
        for (int index = 0; index < PAST_INITIAL_CAPACITY; index++) {
            stack.push(index);
        }

        for (int index = PAST_INITIAL_CAPACITY - 1; index >= 0; index--) {
            assertEquals(index, stack.pop());
        }

        assertTrue(stack.isEmpty());
    }
}
