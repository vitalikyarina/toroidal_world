package com.toroidalworld.net;

import java.util.HashMap;
import java.util.Map;

final class StartupRegistry<K, V> {
    private final String subject;

    private volatile Map<K, V> entries = Map.of();

    private boolean closed;

    StartupRegistry(String subject) {
        this.subject = subject;
    }

    synchronized void register(K key, V value) {
        if (closed) {
            throw new IllegalStateException(subject + " must be registered before the server starts.");
        }

        Map<K, V> grown = new HashMap<>(entries);
        grown.put(key, value);
        entries = Map.copyOf(grown);
    }

    Map<K, V> entries() {
        return entries;
    }

    synchronized void close() {
        closed = true;
    }
}
