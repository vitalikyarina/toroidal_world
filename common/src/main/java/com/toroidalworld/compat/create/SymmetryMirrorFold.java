package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.toroidalworld.compat.create.mixin.SymmetryMirrorAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SymmetryMirrorFold {
    public static SymmetryMirror nearestTo(@Nullable Level level, SymmetryMirror mirror, BlockPos anchor) {
        Vec3 canonical = mirror.getPosition();
        Vec3 nearest = CreateSeamFold.nearestCopy(level, Vec3.atLowerCornerOf(anchor), canonical);
        if (nearest.equals(canonical)) {
            return mirror;
        }

        // The stack hands back the mirror it stores, so a folded position set on it would move the player's own
        // mirror a world width every time they build across the seam.
        return SymmetryMirrorAccessor.toroidal$create(
                mirror.getOrientationIndex(), nearest, mirror.typeName(), mirror.enable);
    }

    private SymmetryMirrorFold() {
    }
}
