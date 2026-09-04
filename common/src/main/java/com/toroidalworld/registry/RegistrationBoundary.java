package com.toroidalworld.registry;

import java.util.ArrayList;
import java.util.List;

public final class RegistrationBoundary {
    public static final RegistrationBoundary STARTUP = new RegistrationBoundary();

    private final List<StartupRegistry<?, ?>> enrolled = new ArrayList<>();

    private boolean closed;

    RegistrationBoundary() {
    }

    synchronized void enrol(StartupRegistry<?, ?> registry) {
        if (closed) {
            registry.close();
            return;
        }

        enrolled.add(registry);
    }

    public synchronized void close() {
        closed = true;
        enrolled.forEach(StartupRegistry::close);
    }
}
