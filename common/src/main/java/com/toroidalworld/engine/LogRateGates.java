package com.toroidalworld.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LogRateGates {
    private final Map<Object, LogRateGate> gates = new ConcurrentHashMap<>();

    public boolean tryPass(Object key) {
        return gates.computeIfAbsent(key, ignored -> new LogRateGate()).tryPass();
    }
}
