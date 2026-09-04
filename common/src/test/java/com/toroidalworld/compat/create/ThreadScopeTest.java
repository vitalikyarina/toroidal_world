package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class ThreadScopeTest {
    @Test
    void anUnboundScopeHasNoCurrentValue() {
        assertNull(new ThreadScope<String>().current());
    }

    @Test
    void aNestedWithRestoresTheOuterValue() {
        ThreadScope<String> scope = new ThreadScope<>();

        String seen = scope.with("outer", () -> {
            String inner = scope.with("inner", scope::current);

            return inner + "/" + scope.current();
        });

        assertEquals("inner/outer", seen);
        assertNull(scope.current());
    }

    @Test
    void bindingNullUnbindsForTheBodyAndRestoresAfterIt() {
        ThreadScope<String> scope = new ThreadScope<>();

        String seen = scope.with("outer", () -> {
            String inner = scope.with(null, scope::current);

            return inner + "/" + scope.current();
        });

        assertEquals("null/outer", seen);
    }

    @Test
    void aBodyThatThrowsStillRestoresTheOuterValue() {
        ThreadScope<String> scope = new ThreadScope<>();

        String seen = scope.with("outer", () -> {
            assertThrows(IllegalStateException.class, () -> scope.with("inner", () -> {
                throw new IllegalStateException("body");
            }));

            return scope.current();
        });

        assertEquals("outer", seen);
        assertNull(scope.current());
    }

    @Test
    void aThrowFromTheOutermostBodyUnbindsTheScope() {
        ThreadScope<String> scope = new ThreadScope<>();

        assertThrows(IllegalStateException.class, () -> scope.with("outer", () -> {
            throw new IllegalStateException("body");
        }));

        assertNull(scope.current());
    }

    @Test
    void aBindingOnOneThreadIsInvisibleToAnother() {
        ThreadScope<String> scope = new ThreadScope<>();

        String seen = scope.with("main", () -> {
            AtomicReference<String> elsewhere = new AtomicReference<>();
            onAnotherThread(() -> elsewhere.set(scope.current()));

            return elsewhere.get() + "/" + scope.current();
        });

        assertEquals("null/main", seen);
    }

    private static void onAnotherThread(Runnable body) {
        Thread thread = new Thread(body);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }
}
