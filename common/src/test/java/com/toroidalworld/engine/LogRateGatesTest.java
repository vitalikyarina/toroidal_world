package com.toroidalworld.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LogRateGatesTest {
    private enum Key {
        FIRST,
        SECOND
    }

    @Test
    void eachKeyHoldsItsOwnGate() {
        LogRateGates gates = new LogRateGates();

        assertTrue(gates.tryPass(Key.FIRST));
        assertTrue(gates.tryPass(Key.SECOND));
    }

    @Test
    void theSameKeyTwiceInOneSecondPassesOnce() {
        LogRateGates gates = new LogRateGates();

        assertTrue(gates.tryPass(Key.FIRST));
        assertFalse(gates.tryPass(Key.FIRST));
    }

    @Test
    void aSecondSetKeepsItsOwnGateForTheSameKey() {
        LogRateGates gates = new LogRateGates();
        LogRateGates otherGates = new LogRateGates();
        assertTrue(gates.tryPass(Key.FIRST));
        assertFalse(gates.tryPass(Key.FIRST));

        assertTrue(otherGates.tryPass(Key.FIRST));
    }
}
