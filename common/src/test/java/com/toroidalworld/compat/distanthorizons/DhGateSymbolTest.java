package com.toroidalworld.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DhGateSymbolTest {
    @Test
    void theGateNamesASymbolTheCompiledAgainstDistantHorizonsCarries() {
        assertTrue(DhMixinPlugin.LEVEL_CHUNK_HASH_REPO.carriedBy(DhGateSymbolTest.class.getClassLoader()),
                DhMixinPlugin.LEVEL_CHUNK_HASH_REPO + " is gone from the Distant Horizons this compat compiles "
                        + "against, so its gate would refuse a Distant Horizons that works");
    }
}
