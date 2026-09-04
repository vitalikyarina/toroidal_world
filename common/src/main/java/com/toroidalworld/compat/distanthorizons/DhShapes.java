package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldApi;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
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

    public static ToroidalShape ofFoldedKeys(IDhLevel level) {
        return withFoldedKeys(of(level));
    }

    public static ToroidalShape withFoldedKeys(ToroidalShape shape) {
        if (shape == null) {
            return null;
        }

        boolean folded = DhFold.keysFoldWithoutCollision(shape, DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
        DhProbes.keyFold(shape, folded);
        return folded ? shape : null;
    }

    private DhShapes() {
    }
}
