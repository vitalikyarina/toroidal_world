package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// C2ME's aquifer optimisation moves the per-cell random: vanilla seeds it inside computeSubstance, one
// positionalRandomFactory.at per sample, and that call is what the mod's own AquiferSeamMixin wraps; C2ME overwrites
// computeSubstance and precomputes the whole grid once at construction instead. Exactly one of the two folds can be
// live, and both sides must switch on the SAME condition.
//
// That condition is C2ME's own resolved flag, not the presence of its classes. The class ships in the jar whichever
// way the option is set, so a classpath probe would read "C2ME is here" for a user running optimizeAquifer=false —
// vanilla's computeSubstance survives, this mod's fold is switched off, the C2ME-shaped fold has nothing to attach to,
// and the aquifer splits at the seam with nothing in the log. Reading the flag also inherits everything C2ME folded
// into it: c2me.toml, the system-property override and its own declared incompatibilities.
//
// Reflection, and eagerly: the field is a compile-time constant on C2ME's side, so a direct reference would be inlined
// at our compile time and never read theirs. Loading the class here runs C2ME's config bootstrap, which is what C2ME's
// own mixin plugin does at this very phase (ModuleMixinPlugin.onLoad reads ModuleEntryPoint.enabled the same way).
public final class C2meAquifer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG_CLASS = "com.ishland.c2me.opts.worldgen.vanilla.common.Config";
    private static final String OPTIMIZE_AQUIFER_FIELD = "optimizeAquifer";

    private static final boolean OPTIMIZED = readOptimizeAquifer();

    // True when C2ME owns the aquifer's per-cell random — the compat fold applies and the vanilla-shaped one stands
    // down. False both when C2ME is absent and when it is present with the optimisation off.
    public static boolean optimizesAquifer() {
        return OPTIMIZED;
    }

    private static boolean readOptimizeAquifer() {
        try {
            boolean optimized = Class.forName(CONFIG_CLASS).getField(OPTIMIZE_AQUIFER_FIELD).getBoolean(null);
            LOGGER.info("[c2me-compat] gate c2me_present=true optimize_aquifer={}", optimized);
            return optimized;
        } catch (ClassNotFoundException absent) {
            return false;
        } catch (ReflectiveOperationException | LinkageError changed) {
            // The field moved or its type changed: assume C2ME still owns computeSubstance, because that is the
            // assumption whose failure is loud. Standing down here would leave the seam unfolded and silent.
            LOGGER.warn("[c2me-compat] cannot read {}.{}, assuming C2ME owns the aquifer",
                    CONFIG_CLASS, OPTIMIZE_AQUIFER_FIELD, changed);
            return true;
        }
    }

    private C2meAquifer() {
    }
}
