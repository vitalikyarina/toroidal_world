package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class StartupRegistryTest {
    private static final String SUBJECT = "Test rewriters";

    @Test
    void aRegisteredValueIsThereToRead() {
        StartupRegistry<String, Integer> registry = new StartupRegistry<>(SUBJECT);

        registry.register("one", 1);

        assertEquals(Integer.valueOf(1), registry.entries().get("one"));
    }

    @Test
    void anUnregisteredKeyReadsAsNull() {
        StartupRegistry<String, Integer> registry = new StartupRegistry<>(SUBJECT);

        assertNull(registry.entries().get("one"));
    }

    @Test
    void theEntriesHandedToAReaderAreImmutable() {
        StartupRegistry<String, Integer> registry = new StartupRegistry<>(SUBJECT);
        registry.register("one", 1);

        Map<String, Integer> entries = registry.entries();

        assertThrows(UnsupportedOperationException.class, () -> entries.put("two", 2));
    }

    @Test
    void aRegistrationLeavesASnapshotAlreadyHandedOutAlone() {
        StartupRegistry<String, Integer> registry = new StartupRegistry<>(SUBJECT);
        registry.register("one", 1);
        Map<String, Integer> held = registry.entries();

        registry.register("two", 2);

        assertEquals(1, held.size());
        assertEquals(2, registry.entries().size());
    }

    @Test
    void aRegistrationAfterTheCloseFailsAndNamesTheBoundary() {
        StartupRegistry<String, Integer> registry = new StartupRegistry<>(SUBJECT);

        registry.close();

        IllegalStateException refused =
                assertThrows(IllegalStateException.class, () -> registry.register("one", 1));
        assertTrue(refused.getMessage().startsWith(SUBJECT), refused.getMessage());
        assertTrue(refused.getMessage().contains("before the server starts"), refused.getMessage());
    }

    @Test
    void whatWasRegisteredBeforeTheCloseIsStillRead() {
        StartupRegistry<String, Integer> registry = new StartupRegistry<>(SUBJECT);
        registry.register("one", 1);

        registry.close();

        assertEquals(Integer.valueOf(1), registry.entries().get("one"));
    }

    @Test
    void closingTwiceLeavesTheRegistryClosed() {
        StartupRegistry<String, Integer> registry = new StartupRegistry<>(SUBJECT);
        registry.close();

        registry.close();

        assertThrows(IllegalStateException.class, () -> registry.register("one", 1));
    }
}
