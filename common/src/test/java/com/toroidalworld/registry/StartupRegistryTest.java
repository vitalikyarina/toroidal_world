package com.toroidalworld.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class StartupRegistryTest {
    private static final String SUBJECT = "Test rewriters";

    private final RegistrationBoundary boundary = new RegistrationBoundary();

    private StartupRegistry<String, Integer> registry() {
        return new StartupRegistry<>(boundary, SUBJECT);
    }

    @Test
    void aRegisteredValueIsThereToRead() {
        StartupRegistry<String, Integer> registry = registry();

        registry.register("one", 1);

        assertEquals(Integer.valueOf(1), registry.entries().get("one"));
    }

    @Test
    void anUnregisteredKeyReadsAsNull() {
        assertNull(registry().entries().get("one"));
    }

    @Test
    void theEntriesHandedToAReaderAreImmutable() {
        StartupRegistry<String, Integer> registry = registry();
        registry.register("one", 1);

        Map<String, Integer> entries = registry.entries();

        assertThrows(UnsupportedOperationException.class, () -> entries.put("two", 2));
    }

    @Test
    void aRegistrationLeavesASnapshotAlreadyHandedOutAlone() {
        StartupRegistry<String, Integer> registry = registry();
        registry.register("one", 1);
        Map<String, Integer> held = registry.entries();

        registry.register("two", 2);

        assertEquals(1, held.size());
        assertEquals(2, registry.entries().size());
    }

    @Test
    void aRegistrationAfterTheCloseFailsAndNamesTheBoundary() {
        StartupRegistry<String, Integer> registry = registry();

        boundary.close();

        IllegalStateException refused =
                assertThrows(IllegalStateException.class, () -> registry.register("one", 1));
        assertTrue(refused.getMessage().startsWith(SUBJECT), refused.getMessage());
        assertTrue(refused.getMessage().contains("before the server starts"), refused.getMessage());
    }

    @Test
    void whatWasRegisteredBeforeTheCloseIsStillRead() {
        StartupRegistry<String, Integer> registry = registry();
        registry.register("one", 1);

        boundary.close();

        assertEquals(Integer.valueOf(1), registry.entries().get("one"));
    }

    @Test
    void everyRegistryEnrolledInTheBoundaryClosesWithIt() {
        StartupRegistry<String, Integer> first = registry();
        StartupRegistry<String, Integer> second = registry();
        first.register("one", 1);
        second.register("two", 2);

        boundary.close();

        assertThrows(IllegalStateException.class, () -> first.register("three", 3));
        assertThrows(IllegalStateException.class, () -> second.register("four", 4));
        assertEquals(Integer.valueOf(1), first.entries().get("one"));
        assertEquals(Integer.valueOf(2), second.entries().get("two"));
    }

    @Test
    void aRegistryBuiltAfterTheCloseIsBornClosed() {
        boundary.close();

        StartupRegistry<String, Integer> late = registry();

        assertThrows(IllegalStateException.class, () -> late.register("one", 1));
        assertTrue(late.entries().isEmpty());
    }

    @Test
    void closingTwiceLeavesTheRegistriesClosed() {
        StartupRegistry<String, Integer> registry = registry();
        boundary.close();

        boundary.close();

        assertThrows(IllegalStateException.class, () -> registry.register("one", 1));
    }
}
