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
            setPlatform(knot);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e instanceof InvocationTargetException invocation ? invocation.getCause() : e;
            throw new IllegalStateException("Vanilla bootstrap under Fabric Loader failed", cause);
        }
    }

    private static void setPlatform(ClassLoader knot) throws ReflectiveOperationException {
        Class<?> platform = Class.forName("com.toroidalworld.platform.Platform", true, knot);
        Object fabricPlatform = Class.forName("com.toroidalworld.platform.FabricPlatform", true, knot)
                .getConstructor().newInstance();
        Class.forName("com.toroidalworld.platform.Platforms", true, knot).getMethod("set", platform)
                .invoke(null, fabricPlatform);
    }
}
