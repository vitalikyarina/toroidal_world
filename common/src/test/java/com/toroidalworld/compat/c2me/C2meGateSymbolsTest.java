package com.toroidalworld.compat.c2me;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.compat.ModSymbol;

class C2meGateSymbolsTest {
    private static final ClassLoader LOADER = C2meGateSymbolsTest.class.getClassLoader();

    @Test
    void everyC2meGateNamesASymbolTheCompiledAgainstModulesCarry() {
        for (ModSymbol symbol : List.of(C2meChunkSystem.CHUNK_SYSTEM_TACS, C2meNoTickVd.NO_TICK_LOADER_TACS,
                C2meDfc.AST_ENTRY, C2meOctaveNoise.OCTAVE_SAMPLER_VALUE, C2meAquifer.SAMPLER_INIT_HANDLER)) {
            assertTrue(symbol.carriedBy(LOADER),
                    symbol + " is gone from the C2ME modules this compat compiles against, so its gate would refuse "
                            + "a C2ME that works");
        }
    }
}
