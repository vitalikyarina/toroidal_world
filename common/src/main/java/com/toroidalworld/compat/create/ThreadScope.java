package com.toroidalworld.compat.create;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

public final class ThreadScope<T> {
    private final ThreadLocal<@Nullable T> bound = new ThreadLocal<>();

    public <R> R with(@Nullable T value, Supplier<R> body) {
        T previous = bound.get();
        rebind(value);
        try {
            return body.get();
        } finally {
            rebind(previous);
        }
    }

    public @Nullable T current() {
        return bound.get();
    }

    private void rebind(@Nullable T value) {
        if (value == null) {
            bound.remove();
        } else {
            bound.set(value);
        }
    }
}
