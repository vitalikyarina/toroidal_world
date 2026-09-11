package com.toroidalworld.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.compat.ModSymbol;

class XaeroGateSymbolsTest {
    private static final ClassLoader LOADER = XaeroGateSymbolsTest.class.getClassLoader();

    @Test
    void bothGatesNameASymbolTheCompiledAgainstXaeroCarries() {
        for (ModSymbol symbol : List.of(XaeroMixinPlugin.WAYPOINT_SCALED_X, XaeroMixinPlugin.WORLDMAP_CAMERA_X)) {
            assertTrue(symbol.carriedBy(LOADER),
                    symbol + " is gone from the Xaero jars this compat compiles against, so its gate would refuse "
                            + "an Xaero that works");
        }
    }
}
