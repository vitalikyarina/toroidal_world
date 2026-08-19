package com.toroidalworld.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class LogRateGate {
    private static final long INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);

    private final AtomicLong lastPassNanos = new AtomicLong(System.nanoTime() - INTERVAL_NANOS);

    public boolean tryPass() {
        long now = System.nanoTime();
        long prev = lastPassNanos.get();
        return now - prev >= INTERVAL_NANOS && lastPassNanos.compareAndSet(prev, now);
    }
}
