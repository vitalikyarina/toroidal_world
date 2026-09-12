package com.toroidalworld.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ObjectStackTest {
    private static final int PAST_INITIAL_CAPACITY = 9;

    @Test
    void popReturnsTheLastPushedValue() {
        ObjectStack<String> stack = new ObjectStack<>();
        stack.push("first");
        stack.push("second");

        assertEquals("second", stack.pop());
        assertEquals("first", stack.pop());
    }

    @Test
    void growingPastTheInitialCapacityKeepsEveryValue() {
        ObjectStack<Integer> stack = new ObjectStack<>();
        for (int index = 0; index < PAST_INITIAL_CAPACITY; index++) {
            stack.push(index);
        }

        for (int index = PAST_INITIAL_CAPACITY - 1; index >= 0; index--) {
            assertEquals(index, stack.pop());
        }
    }
}
