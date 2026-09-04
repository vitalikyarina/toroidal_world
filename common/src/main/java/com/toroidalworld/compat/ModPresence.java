package com.toroidalworld.compat;

import org.slf4j.Logger;

public final class ModPresence {
    private final Logger logger;
    private final ClassLoader classLoader;
    private final String resource;
    private final String gateLabel;

    private boolean probed;
    private boolean present;

    public static ModPresence of(Logger logger, String resource, String gateLabel) {
        return new ModPresence(logger, ModPresence.class.getClassLoader(), resource, gateLabel);
    }

    public static boolean probe(String resource) {
        return probe(ModPresence.class.getClassLoader(), resource);
    }

    static boolean probe(ClassLoader classLoader, String resource) {
        return classLoader.getResource(resource) != null;
    }

    ModPresence(Logger logger, ClassLoader classLoader, String resource, String gateLabel) {
        this.logger = logger;
        this.classLoader = classLoader;
        this.resource = resource;
        this.gateLabel = gateLabel;
    }

    public synchronized boolean present() {
        if (!this.probed) {
            this.present = probe(this.classLoader, this.resource);
            this.probed = true;
            this.logger.info("{}={}", this.gateLabel, this.present);
        }

        return this.present;
    }
}
