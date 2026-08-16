package com.toroidalworld.compat.c2me;

import java.lang.reflect.Field;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// Whether C2ME's native math owns the End's island field. Its opts/natives_math module overwrites
// DensityFunctions$EndIslandDensityFunction.compute with a SIMD sample of its own, from a config of priority 1100
// against this mod's default 1000 — so the vanilla-shaped fold is applied first and then replaced, and the islands
// stop tiling at the seam with nothing in the log. Exactly one of the two folds can be live, and both sides switch on
// this one condition.
//
// The module's own resolved flag, not the presence of its classes: the module ships in the jar whichever way it
// resolves, and it switches itself off on a machine whose CPU it cannot use or from c2me.toml. A classpath probe would
// read "C2ME owns compute" for a player running without native acceleration, stand the vanilla-shaped fold down, and
// leave the compat-shaped one attached to a method nobody overwrote.
//
// Reflection on a private static field, eagerly, which is what C2ME's own ModuleMixinPlugin does at this very phase.
public final class C2meNativesMath {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String ENTRY_POINT_CLASS = "com.ishland.c2me.opts.natives_math.ModuleEntryPoint";
    private static final String ENABLED_FIELD = "enabled";

    private static final boolean ENABLED = readEnabled();

    // True when C2ME's native math owns compute — the compat fold applies and the vanilla-shaped one stands down.
    // False both when C2ME is absent and when the module resolved itself off.
    public static boolean enabled() {
        return ENABLED;
    }

    private static boolean readEnabled() {
        try {
            Field enabled = Class.forName(ENTRY_POINT_CLASS).getDeclaredField(ENABLED_FIELD);
            enabled.setAccessible(true);
            boolean owned = enabled.getBoolean(null);
            LOGGER.info("[c2me-compat] gate natives_math_enabled={}", owned);
            return owned;
        } catch (ClassNotFoundException absent) {
            return false;
        } catch (ReflectiveOperationException | LinkageError changed) {
            // The field moved or its type changed: assume C2ME still owns compute. The compat-shaped fold wraps the
            // method whatever body it ends up with, so guessing this way costs nothing when it is wrong, while the
            // other guess would leave the seam unfolded and silent.
            LOGGER.warn("[c2me-compat] cannot read {}.{}, assuming C2ME owns the End islands",
                    ENTRY_POINT_CLASS, ENABLED_FIELD, changed);
            return true;
        }
    }

    private C2meNativesMath() {
    }
}
