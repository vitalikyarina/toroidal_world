package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.api.v1.ToroidalWorldApi;
import com.toroidalworld.compat.ClientShapes;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.world.level.Level;

public final class DhShapes {
    public static ToroidalShape of(IDhLevel level) {
        return level == null ? null : of(level.getLevelWrapper());
    }

    public static ToroidalShape of(ILevelWrapper wrapper) {
        return shapeOf(mcLevel(wrapper));
    }

    public static ToroidalShape clientFrame(ILevelWrapper wrapper) {
        Level mcLevel = mcLevel(wrapper);
        return mcLevel != null && mcLevel.isClientSide() ? shapeOf(mcLevel) : null;
    }

    private static Level mcLevel(ILevelWrapper wrapper) {
        return wrapper != null && wrapper.getWrappedMcObject() instanceof Level mcLevel ? mcLevel : null;
    }

    private static ToroidalShape shapeOf(Level mcLevel) {
        if (mcLevel == null) {
            return null;
        }

        ToroidalShape shape = mcLevel.isClientSide()
                ? ClientShapes.of(mcLevel)
                : ToroidalWorldApi.shapeOf(mcLevel).orElse(null);
        if (shape != null) {
            DhProbes.keyPeriod(shape, DhKeys.LEAF);
        }

        return shape;
    }

    private DhShapes() {
    }
}
