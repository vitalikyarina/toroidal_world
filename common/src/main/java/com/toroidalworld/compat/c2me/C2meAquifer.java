package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModSymbol;

public final class C2meAquifer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG_CLASS = "com.ishland.c2me.opts.worldgen.vanilla.common.Config";
    private static final String OPTIMIZE_AQUIFER_FIELD = "optimizeAquifer";

    static final ModSymbol SAMPLER_INIT_HANDLER = new ModSymbol(
            "com/ishland/c2me/opts/worldgen/vanilla/mixin/aquifer/MixinAquiferSamplerImpl", "onInit",
            "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");

    private static final boolean OPTIMIZED = readOptimizeAquifer();

    public static boolean optimizesAquifer() {
        return OPTIMIZED;
    }

    private static boolean readOptimizeAquifer() {
        try {
            boolean optimized = Class.forName(CONFIG_CLASS).getField(OPTIMIZE_AQUIFER_FIELD).getBoolean(null);
            if (!optimized) {
                LOGGER.info("[c2me-compat] gate c2me_present=true optimize_aquifer=false");
                return false;
            }

            if (!SAMPLER_INIT_HANDLER.carriedBy(C2meAquifer.class.getClassLoader())) {
                LOGGER.warn("[c2me-compat] gate c2me_present=true optimize_aquifer=true symbol_present=false symbol={}",
                        SAMPLER_INIT_HANDLER);
                return false;
            }

            LOGGER.info("[c2me-compat] gate c2me_present=true optimize_aquifer=true symbol_present=true symbol={}",
                    SAMPLER_INIT_HANDLER);
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        } catch (ReflectiveOperationException | LinkageError changed) {
            // A read failure assumes C2ME still owns computeSubstance: that guess fails loudly, while standing down leaves the seam unfolded and silent.
            LOGGER.warn("[c2me-compat] cannot read {}.{}, assuming C2ME owns the aquifer",
                    CONFIG_CLASS, OPTIMIZE_AQUIFER_FIELD, changed);
            return true;
        }
    }

    private C2meAquifer() {
    }
}
