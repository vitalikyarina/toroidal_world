package com.toroidalworld;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class FabricVanillaBootstrap implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        ClassLoader knot = context.getRequiredTestClass().getClassLoader();
        try {
            Class.forName("net.minecraft.SharedConstants", true, knot).getMethod("tryDetectVersion").invoke(null);
            Class.forName("net.minecraft.server.Bootstrap", true, knot).getMethod("bootStrap").invoke(null);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e instanceof InvocationTargetException invocation ? invocation.getCause() : e;
            throw new IllegalStateException("Vanilla bootstrap under Fabric Loader failed", cause);
        }
    }
}
