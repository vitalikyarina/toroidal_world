package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LogRateGateTest {
    @Test
    void theFirstCallPasses() {
        assertTrue(new LogRateGate().tryPass());
    }

    @Test
    void aBurstPassesExactlyOnce() {
        LogRateGate gate = new LogRateGate();
        int passes = 0;
        for (int call = 0; call < 1000; call++) {
            if (gate.tryPass()) {
                passes++;
            }
        }

        assertEquals(1, passes);
    }

    @Test
    void theGateReopensAfterTheInterval() throws InterruptedException {
        LogRateGate gate = new LogRateGate();
        assertTrue(gate.tryPass());
        assertFalse(gate.tryPass());

        Thread.sleep(1100);
        assertTrue(gate.tryPass());
    }
}
