package com.toroidalworld.registry;

import java.util.HashMap;
import java.util.Map;

public final class StartupRegistry<K, V> {
    private final String subject;

    private volatile Map<K, V> entries = Map.of();

    private boolean closed;

    public StartupRegistry(String subject) {
        this(RegistrationBoundary.STARTUP, subject);
    }

    StartupRegistry(RegistrationBoundary boundary, String subject) {
        this.subject = subject;
        boundary.enrol(this);
    }

    public synchronized void register(K key, V value) {
        if (closed) {
            throw new IllegalStateException(subject + " must be registered before the server starts.");
        }

        Map<K, V> grown = new HashMap<>(entries);
        grown.put(key, value);
        entries = Map.copyOf(grown);
    }

    public Map<K, V> entries() {
        return entries;
    }

    synchronized void close() {
        closed = true;
    }
}
