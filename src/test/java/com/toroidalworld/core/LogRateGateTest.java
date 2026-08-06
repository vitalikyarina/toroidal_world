package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// The gate reads the real clock — it has no seam to inject a fake one through, and growing one for the tests would
// change production for nothing production needs. So the reopening case simply waits the interval out: one slow test,
// a real second, rather than a clock abstraction on a two-line class.
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
