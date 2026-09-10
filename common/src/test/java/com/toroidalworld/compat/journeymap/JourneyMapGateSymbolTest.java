package com.toroidalworld.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JourneyMapGateSymbolTest {
    @Test
    void theGateNamesASymbolTheCompiledAgainstJourneyMapCarries() {
        assertTrue(JourneyMapMixinPlugin.MAP_RENDERER_CENTRE.carriedBy(JourneyMapGateSymbolTest.class.getClassLoader()),
                JourneyMapMixinPlugin.MAP_RENDERER_CENTRE + " is gone from the JourneyMap this compat compiles "
                        + "against, so its gate would refuse a JourneyMap that works");
    }
}
