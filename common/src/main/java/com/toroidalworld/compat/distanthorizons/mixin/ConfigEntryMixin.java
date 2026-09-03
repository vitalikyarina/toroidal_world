package com.toroidalworld.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.distanthorizons.DhClientShapes;
import com.toroidalworld.compat.distanthorizons.DhFold;
import com.toroidalworld.compat.distanthorizons.DhProbes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;

@Mixin(ConfigEntry.class)
public class ConfigEntryMixin {
    @WrapMethod(method = "get()Ljava/lang/Object;")
    private Object toroidal$capTheLodRadius(Operation<Object> original) {
        Object value = original.call();
        if ((Object) this != Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistanceRadius
                || !(value instanceof Integer configChunks)) {
            return value;
        }

        ToroidalShape shape = DhClientShapes.ofCurrentLevel();
        if (shape == null) {
            return value;
        }

        int cap = DhFold.radiusCapChunks(shape);
        if (configChunks <= cap) {
            return value;
        }

        DhProbes.radiusCapped(configChunks, cap);
        return cap;
    }
}
