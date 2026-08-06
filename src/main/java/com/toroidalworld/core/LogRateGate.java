package com.toroidalworld.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// At most one passage per second. A broken invariant on a packet path floods every packet at once, and the first
// line already names the break; the rest is volume. Callers only ask once a violation is in hand, so a healthy path
// never pays for the clock read.
public final class LogRateGate {
    private static final long INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);

    private final AtomicLong lastPassNanos = new AtomicLong(System.nanoTime() - INTERVAL_NANOS);

    public boolean tryPass() {
        long now = System.nanoTime();
        long prev = lastPassNanos.get();
        return now - prev >= INTERVAL_NANOS && lastPassNanos.compareAndSet(prev, now);
    }
}
