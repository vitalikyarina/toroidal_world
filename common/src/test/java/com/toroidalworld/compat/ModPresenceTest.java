package com.toroidalworld.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

class ModPresenceTest {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SHIPPED_CLASS = "com/toroidalworld/compat/ModPresence.class";
    private static final String ABSENT_CLASS = "com/toroidalworld/compat/NoSuchModEntryPoint.class";

    @Test
    void aResourceOnTheClasspathReadsAsPresent() {
        assertTrue(ModPresence.of(LOGGER, SHIPPED_CLASS, "[test-compat] gate shipped_present").present(),
                "the mod's own class file resolves through the loader the factory picks");
    }

    @Test
    void aResourceNothingShipsReadsAsAbsent() {
        assertFalse(ModPresence.of(LOGGER, ABSENT_CLASS, "[test-compat] gate absent_present").present(),
                "no jar on the classpath carries that class file");
    }

    @Test
    void theProbeAsksTheClassLoaderOnce() {
        CountingLoader loader = new CountingLoader(SHIPPED_CLASS);
        ModPresence gate = new ModPresence(LOGGER, loader, SHIPPED_CLASS, "[test-compat] gate once_present");

        assertTrue(gate.present());
        assertTrue(gate.present());

        assertEquals(1, loader.lookups, "the second present() answers from the value the first one probed");
        assertEquals(SHIPPED_CLASS, loader.asked, "the probe asks for the resource its gate names");
    }

    private static final class CountingLoader extends ClassLoader {
        private final String resolvable;

        private int lookups;
        private String asked;

        private CountingLoader(String resolvable) {
            super(null);
            this.resolvable = resolvable;
        }

        @Override
        public URL getResource(String name) {
            this.lookups++;
            this.asked = name;

            return name.equals(this.resolvable) ? ModPresenceTest.class.getResource("ModPresenceTest.class") : null;
        }
    }
}
