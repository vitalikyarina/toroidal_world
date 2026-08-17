package com.toroidalworld.compat.create;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// Whether Create is installed. It carries no option that could switch off the code the seam folds attach to, so the
// mod being on the classpath is the whole condition. Detection is a classpath resource probe rather than a loader API
// or Class.forName: mixin config plugins run before mod initialization on both loaders, and looking a .class resource
// up loads nothing.
public final class CreateMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CREATE_CLASS = "com/simibubi/create/Create.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    private static boolean probe() {
        boolean present = CreateMod.class.getClassLoader().getResource(CREATE_CLASS) != null;
        LOGGER.info("[create-compat] gate create_present={}", present);
        return present;
    }

    private CreateMod() {
    }
}
