package com.toroidalworld.compat.c2me;

import java.lang.reflect.Field;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public final class C2meNativesMath {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String ENTRY_POINT_CLASS = "com.ishland.c2me.opts.natives_math.ModuleEntryPoint";
    private static final String ENABLED_FIELD = "enabled";

    private static final boolean ENABLED = readEnabled(C2meNativesMath.class.getClassLoader());

    public static boolean enabled() {
        return ENABLED;
    }

    static boolean readEnabled(ClassLoader classLoader) {
        try {
            Field enabled = Class.forName(ENTRY_POINT_CLASS, true, classLoader).getDeclaredField(ENABLED_FIELD);
            enabled.setAccessible(true);
            boolean owned = enabled.getBoolean(null);
            LOGGER.info("[c2me-compat] gate natives_math_enabled={}", owned);
            return owned;
        } catch (ClassNotFoundException absent) {
            return false;
        } catch (UnsupportedClassVersionError unloadable) {
            LOGGER.info("[c2me-compat] gate natives_math_enabled=false unloadable={}", unloadable.getMessage());
            return false;
        } catch (ReflectiveOperationException | LinkageError changed) {
            LOGGER.warn("[c2me-compat] cannot read {}.{}, assuming C2ME owns the End islands",
                    ENTRY_POINT_CLASS, ENABLED_FIELD, changed);
            return true;
        }
    }

    private C2meNativesMath() {
    }
}
