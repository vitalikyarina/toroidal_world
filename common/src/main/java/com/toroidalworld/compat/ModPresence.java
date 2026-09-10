package com.toroidalworld.compat;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class ModPresence {
    private final Logger logger;
    private final ClassLoader classLoader;
    private final String resource;
    private final String gateLabel;
    private final @Nullable ModSymbol required;

    private boolean probed;
    private boolean present;

    public static ModPresence of(Logger logger, String resource, String gateLabel) {
        return new ModPresence(logger, ModPresence.class.getClassLoader(), resource, gateLabel, null);
    }

    public static ModPresence of(Logger logger, String resource, String gateLabel, ModSymbol required) {
        return new ModPresence(logger, ModPresence.class.getClassLoader(), resource, gateLabel, required);
    }

    public static boolean probe(String resource) {
        return probe(ModPresence.class.getClassLoader(), resource);
    }

    static boolean probe(ClassLoader classLoader, String resource) {
        return classLoader.getResource(resource) != null;
    }

    ModPresence(Logger logger, ClassLoader classLoader, String resource, String gateLabel,
            @Nullable ModSymbol required) {
        this.logger = logger;
        this.classLoader = classLoader;
        this.resource = resource;
        this.gateLabel = gateLabel;
        this.required = required;
    }

    public synchronized boolean present() {
        if (!this.probed) {
            this.present = resolve();
            this.probed = true;
        }

        return this.present;
    }

    private boolean resolve() {
        if (!probe(this.classLoader, this.resource)) {
            this.logger.info("{}=false", this.gateLabel);
            return false;
        }

        if (this.required == null) {
            this.logger.info("{}=true", this.gateLabel);
            return true;
        }

        if (!this.required.carriedBy(this.classLoader)) {
            this.logger.warn("{}=true symbol_present=false symbol={}", this.gateLabel, this.required);
            return false;
        }

        this.logger.info("{}=true symbol_present=true symbol={}", this.gateLabel, this.required);
        return true;
    }
}
