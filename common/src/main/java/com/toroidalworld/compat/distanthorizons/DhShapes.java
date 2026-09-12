package com.toroidalworld.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.api.v1.ToroidalWorldApi;
import com.toroidalworld.compat.ClientShapes;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.world.level.Level;

public final class DhShapes {
    public static @Nullable ToroidalShape of(IDhLevel level) {
        return level == null ? null : of(level.getLevelWrapper());
    }

    public static @Nullable ToroidalShape of(ILevelWrapper wrapper) {
        return shapeOf(mcLevel(wrapper));
    }

    public static @Nullable ToroidalShape clientFrame(ILevelWrapper wrapper) {
        Level mcLevel = mcLevel(wrapper);
        return mcLevel != null && mcLevel.isClientSide() ? shapeOf(mcLevel) : null;
    }

    private static @Nullable Level mcLevel(ILevelWrapper wrapper) {
        return wrapper != null && wrapper.getWrappedMcObject() instanceof Level mcLevel ? mcLevel : null;
    }

    private static @Nullable ToroidalShape shapeOf(Level mcLevel) {
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
