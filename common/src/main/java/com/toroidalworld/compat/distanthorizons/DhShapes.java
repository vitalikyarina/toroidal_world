package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldApi;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.world.level.Level;

public final class DhShapes {
    public static ToroidalShape of(IDhLevel level) {
        return level == null ? null : of(level.getLevelWrapper());
    }

    public static ToroidalShape of(ILevelWrapper wrapper) {
        if (wrapper == null || !(wrapper.getWrappedMcObject() instanceof Level mcLevel)) {
            return null;
        }

        return mcLevel.isClientSide() ? DhClientShapes.of(mcLevel) : ToroidalWorldApi.shapeOf(mcLevel).orElse(null);
    }

    private DhShapes() {
    }
}
